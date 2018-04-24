/*******************************************************************************
 * Copyright (C) 2017 Push Technology Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package com.pushtechnology.diffusion.examples;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.callbacks.Registration;
import com.pushtechnology.diffusion.client.features.control.topics.TopicNotifications;
import com.pushtechnology.diffusion.client.features.control.topics.TopicNotifications.NotificationRegistration;
import com.pushtechnology.diffusion.client.features.control.topics.TopicNotifications.TopicNotificationListener;
import com.pushtechnology.diffusion.client.session.Session;
import com.pushtechnology.diffusion.client.topics.details.TopicSpecification;

/**
 * Examples for using the {@link TopicNotifications} feature to receive
 * notifications for the topic tree.
 *
 * @author Push Technology Limited
 * @since 6.0
 */
public class ControlClientTopicNotifications {

    private final Session session;
    private final TopicNotifications notifications;

    /**
     * Constructor.
     *
     */
    public ControlClientTopicNotifications() {
        this.session =
            Diffusion.sessions().principal("control")
                .password("password")
                .open("ws://diffusion.example.com:80");

        this.notifications = session.feature(TopicNotifications.class);
    }

    /**
     * Basic example of receiving notifications about all topics under a given
     * branch. This registers a long-lived notification listener and selects all
     * topics under the provided root path.
     *
     * With topics at the following paths:
     *
     * <pre>
     * a
     * a/b
     * a/b/c/d
     *
     * e/f
     * e/f/g/h/i
     * </pre>
     *
     * To listen for topic events:
     *
     * <pre>
     * TreeListener listener = new TreeListener() {
     *
     *     public void onTopicAdded(String topicPath,
     *         TopicSpecification specification) {
     *         // Handle topic addition
     *     }
     *
     *     public void onTopicRemoved(String topicPath,
     *         TopicSpecification specification) {
     *         // Handle topic removal
     *     }
     *
     *     public void onClose() {
     *         // The listener has been closed
     *     }
     * };
     *
     * // Get notifications of all topics at or below the topic path "a"
     * topicsForBranch("a", listener);
     *
     * // Get notifications of all topics at or below the topic path "e"
     * topicsForBranch("e", listener);
     * </pre>
     *
     * which would result in each topic path and specification being notified to
     * {@link ControlClientTopicNotifications.TreeListener#onTopicAdded(String, TopicSpecification)
     * onTopicAdded}.
     *
     * @param rootPath the topic path from which to start walking the tree
     * @param listener the listener on which to receive tree structure callbacks
     * @return a closeable allowing the cancellation of the registered listener
     */
    public Closeable topicsForBranch(String rootPath, TreeListener listener) {

        final CompletableFuture<NotificationRegistration> registration =
            notifications.addListener(
                new TopicNotificationListener.Default() {
                    @Override
                    public void onTopicNotification(String topicPath,
                        TopicSpecification spec, NotificationType type) {
                        switch (type) {
                        case ADDED:
                        case SELECTED:
                            listener.onTopicAdded(topicPath, spec);
                            return;
                        case REMOVED:
                        case DESELECTED:
                            listener.onTopicRemoved(topicPath, spec);
                            return;
                        default:
                            // No default action
                        }
                    }

                    @Override
                    public void onClose() {
                        listener.onClose();
                    }
                });

        registration.thenAccept(r -> {
            r.select(rootPath + "//");
        });

        return new Closeable() {
            @Override
            public void close() {
                registration.thenAccept(Registration::close);
            }
        };
    }

    /**
     * Listener for receiving callbacks about the structure of the topic tree.
     */
    interface TreeListener {
        /**
         * Notification that this listener has been closed.
         */
        void onClose();

        /**
         * Notification that a topic has been added.
         *
         * @param topicPath the topic path
         * @param specification the topic specification
         */
        void onTopicAdded(String topicPath, TopicSpecification specification);

        /**
         * Notification that a topic has been removed.
         *
         * @param topicPath the topic path
         * @param specification the topic specification
         */
        void onTopicRemoved(String topicPath, TopicSpecification specification);
    }

    /**
     * Advanced example of walking the topic tree from a given root path in
     * conjunction with a GUI system, with each topic that the GUI is notified
     * about providing a means to descend further down the topic tree in
     * reaction to user-controlled events.
     * <p>
     * With topics at the following paths:
     *
     * <pre>
     * a
     * a/b
     * a/b/c/d
     * a/e
     * a/e/f
     * </pre>
     *
     * Running the following code will result in the GUI being notified of topic
     * {@code "a"}:
     *
     * <pre>
     * // Some abstract GUI reference
     * GUI gui = new GUI() {
     *      void onTopicAdded(
     *          String topicPath,
     *          TopicSpecification specification,
     *          Consumer<Boolean> selectDescendants) {
     *          ...
     *      }
     *
     *      void onTopicRemoved(String topicPath) {
     *          ...
     *      }
     * };
     *
     * walkTree("a", new TreeWalker() {
     *     void onTopicAdded(
     *         TreeNode node,
     *         TopicSpecification specification) {
     *              // Add a selected topic and its specification to the GUI,
     *              // along with a function to toggle selection of
     *              // the node's immediate descendants
     *              gui.onTopicAdded(
     *                  node.getTopicPath(),
     *                  specification,
     *                  node::selectDescendants);
     *     }
     *
     *     void onTopicRemoved(TreeNode node) {
     *         // Remove a selected topic from the GUI
     *         gui.onTopicRemoved(node.getTopicPath());
     *     }
     * });
     * </pre>
     *
     * Once the callback provided to the GUI to select descendants is invoked
     * (such as by a user clicking to 'expand' the tree), the walker will be
     * notified of topics {@code "a/b"} and {@code "a/e"}. For each of those
     * topics, the GUI can choose to select the descendants of that path. When
     * the GUI decides to expand {@code "a/b"}, it will then be informed of
     * topic {@code "a/b/c/d"}.
     * <P>
     * When the returned {@code Closeable} is invoked to close the walker, the
     * GUI will be informed of the removal of all topics. No further
     * interactions will occur.
     *
     * @param rootPath the root topic path from which to begin walking the topic
     *        tree
     * @param walker the tree walker to receive topic notifications
     * @return a closeable to allow deregistration of the tree walker
     * @throws Exception
     */
    public Closeable walkTree(String rootPath, TreeWalker walker)
        throws Exception {

        final InternalListener listener =
            new InternalListener(rootPath, walker);

        final CompletableFuture<NotificationRegistration> registration =
            notifications.addListener(listener);

        registration.thenAccept(listener::initialise);

        return new Closeable() {
            @Override
            public void close() throws IOException {
                registration.thenAccept(Registration::close);
            }
        };
    }

    /**
     * Callback interface for walking the topic tree via {@link walkTree}.
     */
    interface TreeWalker {
        /**
         * Notification that a topic has been added.
         *
         * @param node the tree node for this topic
         * @param specification the topic specification for this topic
         */
        void onTopicAdded(TreeNode node, TopicSpecification specification);

        /**
         * Notification that a topic has been removed.
         *
         * @param node the tree node for this topic
         * @param specification the topic specification for this topic
         */
        void onTopicRemoved(TreeNode node);
    }

    /**
     * Representation of a bound topic path, providing navigation operations for
     * walking further down the tree.
     */
    interface TreeNode {
        /**
         * @return the topic path that this node references
         */
        String getTopicPath();

        /**
         * Toggle whether immediate descendants of this topic node should be
         * selected or not.
         *
         * @param select {@code true} if this node should select immediate
         *        descendants
         */
        void selectDescendants(boolean select);
    }

    /**
     * An internal implementation of TopicNotificationListener, providing a
     * means of registering new listeners for each exposed TreeNode, and
     * maintaining references to child nodes that have been created.
     */
    private static class InternalListener
        extends TopicNotificationListener.Default {

        private final ConcurrentMap<String, InternalTreeNode> topicNodes =
            new ConcurrentHashMap<>();

        private volatile NotificationRegistration registration;

        private final String rootPath;
        private final TreeWalker walker;

        /**
         * Constructor.
         *
         * @param rootPath the root path
         * @param walker the associated tree walker
         */
        InternalListener(String rootPath, TreeWalker walker) {
            this.rootPath = rootPath;
            this.walker = walker;
        }

        public void initialise(NotificationRegistration newRegistration) {
            registration = newRegistration;
            registration.select(rootPath);
        }

        @Override
        public void onTopicNotification(
            String topicPath,
            TopicSpecification specification,
            NotificationType type) {

            if (type == NotificationType.ADDED ||
                type == NotificationType.SELECTED) {
                final InternalTreeNode node =
                    new InternalTreeNode(registration, topicPath);

                topicNodes.put(topicPath, node);
                walker.onTopicAdded(node, specification);
            }
            else {
                walker.onTopicRemoved(topicNodes.remove(topicPath));
            }
        }

        @Override
        public void onDescendantNotification(
            String topicPath,
            NotificationType type) {

            InternalTreeNode parent = null;
            String path = topicPath;

            // Walk up the path until we find the closest registered topic
            while (true) {
                final int index = path.lastIndexOf("/");

                if (index == -1) {
                    break;
                }

                path = path.substring(0, index);
                parent = topicNodes.get(path);

                if (parent != null) {
                    break;
                }
            }

            // If we don't have any parent nodes, then we're at the root path -
            // so just directly select it
            if (parent == null) {
                registration.select(topicPath);
            }
            // Otherwise, add to the registered node, and let the 'select
            // descendants' toggle determine if we select
            else if (type == NotificationType.ADDED ||
                type == NotificationType.SELECTED) {
                parent.addDescendant(topicPath);
            }
            else {
                parent.removeDescendant(topicPath);
            }
        }

        @Override
        public void onClose() {

            final List<String> pathsToRemove =
                new ArrayList<>(topicNodes.keySet());

            // Sort by length &ndash; longer paths are lower in the topic tree,
            // so we remove topics by walking up the
            // tree
            pathsToRemove.sort((s1, s2) -> s1.length() > s2.length() ? -1 :
                s2.length() > s1.length() ? 1 : 0);

            for (String topicPath : pathsToRemove) {
                walker.onTopicRemoved(topicNodes.remove(topicPath));
            }
        }

        private class InternalTreeNode implements TreeNode {

            // Maintain a list of all unselected topics below this node
            private final List<String> descendants = new ArrayList<>();

            private final NotificationRegistration registration;
            private final String topicPath;

            private boolean selectsDescendants = false;

            InternalTreeNode(
                NotificationRegistration registration,
                String topicPath) {
                this.registration = registration;
                this.topicPath = topicPath;
            }

            @Override
            public String getTopicPath() {
                return topicPath;
            }

            @Override
            public synchronized void selectDescendants(boolean selects) {
                selectsDescendants = selects;

                if (selectsDescendants) {
                    for (String descendant : descendants) {
                        registration.select(descendant);
                    }
                }
            }

            public synchronized void addDescendant(String descendantPath) {
                descendants.add(descendantPath);

                if (selectsDescendants) {
                    registration.select(descendantPath);
                }
            }

            public synchronized void removeDescendant(String descendantPath) {
                descendants.remove(descendantPath);

                if (selectsDescendants) {
                    registration.deselect(descendantPath);
                }
            }
        }
    }
}
