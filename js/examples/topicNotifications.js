
var diffusion = require('diffusion');

// Connect to the server. Change these options to suit your own environment.
// Node.js will not accept self-signed certificates by default. If you have
// one of these, set the environment variable NODE_TLS_REJECT_UNAUTHORIZED=0
// before running this example.
diffusion.connect({
    host   : 'diffusion.example.com',
    port   : 443,
    secure : true,
    principal: 'client',
    credentials: 'password'
}).then(async function(session) {
    var TopicNotificationType = session.notifications.TopicNotificationType;

    // A topic notification listener can be used to listen to topic notifications
    var topicNotificationListener = {
        // Called when the session receives a notification for a selected topic
        onTopicNotification: (path, specification, type) => {
            switch (type) {
                case TopicNotificationType.ADDED:
                    console.log(`Topic ${path} has been added`);
                    break;
                case TopicNotificationType.REMOVED:
                    console.log(`Topic ${path} has been removed`);
                    break;
                case TopicNotificationType.SELECTED:
                    console.log(`Topic ${path} existed at the time of the selector registration.`);
                    break;
                case TopicNotificationType.DESELECTED:
                    console.log(`Topic ${path} has been deselected`);
                    break;
            }
        },
        // Called when the session receives a notification for an immediate
        // descendant of a selected topic
        onDescendantNotification: (path, type) => {
            switch (type) {
                case TopicNotificationType.ADDED:
                    console.log(`Topic ${path} has been added`);
                    break;
                case TopicNotificationType.REMOVED:
                    console.log(`Topic ${path} has been removed`);
                    break;
                case TopicNotificationType.SELECTED:
                    console.log(`Topic ${path} existed at the time of the selector registration.`);
                    break;
                case TopicNotificationType.DESELECTED:
                    console.log(`Topic ${path} has been deselected`);
                    break;
                }
        },
        // Called when the listener is closed
        onClose: () => {
            console.log('Topic notification listener has been closed');
        },
        // Called when an error has occurred
        onError(error) {
            console.log('An error has occurred');
        }
    }

    // register the listener
    session.notifications.addListener(topicNotificationListener).then((registration) => {
        // select topics
        // topic notifications will be emitted on all selected topics
        registration.select('?foo/bar//');
    });
});
