# The following two variables must be set.
#
# Directory containing the C client include files.
# DIFFUSION_C_CLIENT_INCDIR	=
#
# Directory containing libdiffusion.a
# DIFFUSION_C_CLIENT_LIBDIR	=

ifndef DIFFUSION_C_CLIENT_INCDIR
$(error DIFFUSION_C_CLIENT_INCDIR is not set)
endif

ifndef DIFFUSION_C_CLIENT_LIBDIR
$(error DIFFUSION_C_CLIENT_LIBDIR is not set)
endif

ifndef TARGETDIR
$(error TARGETDIR is not set)
endif

# Extra definitions from parent directory, if they exist.
-include makefile.defs

CFLAGS		+=	$(INCLUDES) \
				-g -std=c99 -D_POSIX_C_SOURCE=200112L -D_XOPEN_SOURCE=700 \
				-Wall -Werror -Wno-error=deprecated-declarations \
				-I$(DIFFUSION_C_CLIENT_INCDIR)

LDFLAGS		+= 	-lpthread -lpcre -lcurl -lz \
				$(DIFFUSION_C_CLIENT_LIBDIR)/libdiffusion.a \
				$(LIBS)

# Detect the platform the Diffusion Client will be running on
PLATFORM	= 	$(shell uname -s | tr '[A-Z]' '[a-z]' | sed -e 's/darwin/osx/')

ifeq ($(PLATFORM),osx)
	CC		= 	clang
else
	# If not MacOS, add '-lrt' to the linker flags
	CC		= 	gcc
	LDFLAGS	+=	-lrt
endif

ARFLAGS		+=
SOURCES 	=	connect-async.c \
				connect.c \
				reconnect.c \
				features/authentication_control/auth-service.c \
				features/client-control-change-roles-with-filter.c \
				features/client-control-change-roles-with-session.c \
				features/client-control-close-with-filter.c \
				features/client-control-close-with-session.c \
				features/client_control/get-session-properties.c \
				features/client_control/session-properties-listener.c \
				features/messaging/send-request-to-filter.c \
				features/messaging/send-request-to-path.c \
				features/messaging/send-request-to-session.c \
				features/metrics/session-metric-collector.c \
				features/metrics/topic-metric-collector.c \
				features/remote_servers/remote-servers.c \
				features/security/change-principal.c \
				features/session_trees/session-trees.c \
				features/subscription_control/subscription-control.c \
				features/system_authentication_control/system-auth-control.c \
				features/time_series/time-series-timestamp-append.c \
				features/time_series/time-series-range-query.c \
				features/time_series/time-series-append.c \
				features/time_series/time-series-edit.c \
				features/topic_control/missing-topic-notification.c \
				features/topic_control/add-topics.c \
				features/topic_update/update-record.c \
				features/topic_update/topic-update.c \
				features/topic_update/topic-update-stream.c \
				features/topic_update/topic-update-with-constraint.c \
				features/topic_update/topic-update-add-and-set.c \
				features/topic_views/topic-views.c \
				features/topic_views/topic-views-get.c \
				features/topic_views/topic-views-remove.c \
				features/topic_views/topic-views-list.c \
				features/topics/subscribe.c \
				features/topics/subscribe-multiple.c \
				features/topics/recordv2-topics.c \
				features/topics/string-topics.c \
				features/topics/double-topics.c \
				features/topics/fetch-request.c \
				features/topics/int64-topics.c \
				features/topics/binary-topics.c \
				website/publish-subscribe.c \
				website/request-response.c \
				website/rest.c \
				website/time-series.c \
				manual/authentication_control.c \
				manual/client_control.c \
				manual/connecting_basics.c \
				manual/connecting_credentials.c \
				manual/connecting_securely.c \
				manual/fetch_topics.c \
				manual/messaging.c \
				manual/missing_topic_notification.c \
				manual/reconnecting.c \
				manual/reconnection_strategy.c \
				manual/remove_topics.c \
				manual/security_control.c \
				manual/session_locks.c \
				manual/session_properties.c \
				manual/subscribe_to_json.c \
				manual/subscription.c \
				manual/subscription_control.c \
				manual/subscription_control_filter.c \
				manual/system_authentication_control.c \
				manual/timeseries_publish.c \
				manual/timeseries_subscribe.c \
				manual/topic_control.c \
				manual/topic_control_create_and_subscribe_json.c \
				manual/topic_notifications.c \
				manual/topic_update_exclusive.c \
				manual/topic_update_json.c \
				manual/topic_update_non_exclusive.c \
				manual/value_streams.c

OBJDIR		= 	$(TARGETDIR)/objs
BINDIR		= 	$(TARGETDIR)/bin
OBJECTS		= 	$(SOURCES:.c=.o)

TARGETS 	= 	connect-async \
				connect \
				reconnect \
				authentication-control \
				client-control-change-roles-with-filter \
				client-control-change-roles-with-session \
				client-control-close-with-filter \
				client-control-close-with-session \
				client-control-get-session-properties \
				client-control-session-properties-listener \
				messaging-send-request-to-filter \
				messaging-send-request-to-path \
				messaging-send-request-to-session \
				metrics-session-metric-collector \
				metrics-topic-metric-collector \
				remote-servers \
				session-trees \
				security-change-principal \
				subscription-control \
				system-authentication-control \
				time-series-timestamp-append \
				time-series-range-query \
				time-series-append \
				time-series-edit \
				topic-control-missing-topic-notification \
				topic-control-add-topics \
				topic-update-record \
				topic-update \
				topic-update-stream \
				topic-update-with-constraint \
				topic-update-add-and-set \
				topic-views \
				topic-views-get \
				topic-views-remove \
				topic-views-list \
				topics-subscribe \
				topics-subscribe-multiple \
				topics-recordv2 \
				topics-string \
				topics-double \
				topics-fetch \
				topics-int64 \
				topics-binary \
				website-publish-subscribe \
				website-request-response \
				website-rest \
				website-time-series \
				manual-authentication-control \
				manual-client-control \
				manual-connecting-basics \
				manual-connecting-credentials \
				manual-connecting-securely \
				manual-fetch-topics \
				manual-messaging \
				manual-missing-topic-notification \
				manual-reconnecting \
				manual-reconnection-strategy \
				manual-remove-topics \
				manual-security-control \
				manual-session-locks \
				manual-session-properties \
				manual-subscribe-to-json \
				manual-subscription \
				manual-subscription-control \
				manual-subscription-control-filter \
				manual-system-authentication-control \
				manual-timeseries-publish \
				manual-timeseries-subscribe \
				manual-topic-control \
				manual-topic-control-create-and-subscribe-json \
				manual-topic-notifications \
				manual-topic-update-exclusive \
				manual-topic-update-json \
				manual-topic-update-non-exclusive \
				manual-value-streams

.PHONY: all

all: prepare $(TARGETS)

prepare:
		mkdir -p $(OBJDIR) $(BINDIR)

$(OBJDIR)/%.o: %.c
		$(CC) $(CFLAGS) -c -o $@ $<

connect-async: $(OBJDIR)/connect-async.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

connect: $(OBJDIR)/connect.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

reconnect: $(OBJDIR)/reconnect.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

authentication-control: features/authentication_control/auth-service.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-change-roles-with-filter: features/client_control/change-roles-with-filter.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-change-roles-with-session: features/client_control/change-roles-with-session.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-close-with-filter: features/client_control/close-with-filter.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-close-with-session: features/client_control/close-with-session.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-get-session-properties: features/client_control/get-session-properties.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-session-properties-listener: features/client_control/session-properties-listener.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

messaging-send-request-to-filter: features/messaging/send-request-to-filter.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

messaging-send-request-to-path: features/messaging/send-request-to-path.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

messaging-send-request-to-session: features/messaging/send-request-to-session.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

metrics-session-metric-collector: features/metrics/session-metric-collector.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

metrics-topic-metric-collector: features/metrics/topic-metric-collector.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

remote-servers: features/remote_servers/remote-servers.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

session-trees: features/session_trees/session-trees.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

security-change-principal: features/security/change-principal.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

subscription-control: features/subscription_control/subscription-control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

system-authentication-control: features/system_authentication_control/system-auth-control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

time-series-timestamp-append: features/time_series/time-series-timestamp-append.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

time-series-range-query: features/time_series/time-series-range-query.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

time-series-append: features/time_series/time-series-append.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

time-series-edit: features/time_series/time-series-edit.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-control-missing-topic-notification: features/topic_control/missing-topic-notification.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-control-add-topics: features/topic_control/add-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update-record: features/topic_update/update-record.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update: features/topic_update/topic-update.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update-stream: features/topic_update/topic-update-stream.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update-with-constraint: features/topic_update/topic-update-with-constraint.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update-add-and-set: features/topic_update/topic-update-add-and-set.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-views: features/topic_views/topic-views.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-views-get: features/topic_views/topic-views-get.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-views-remove: features/topic_views/topic-views-remove.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-views-list: features/topic_views/topic-views-list.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-subscribe: features/topics/subscribe.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-subscribe-multiple: features/topics/subscribe-multiple.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-recordv2: features/topics/recordv2-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-string: features/topics/string-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-double: features/topics/double-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-fetch: features/topics/fetch-request.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-int64: features/topics/int64-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-binary: features/topics/binary-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

website-publish-subscribe: website/publish-subscribe.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

website-request-response: website/request-response.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

website-rest: website/rest.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

website-time-series: website/time-series.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-authentication-control: manual/authentication_control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-client-control: manual/client_control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-connecting-basics: manual/connecting_basics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-connecting-credentials: manual/connecting_credentials.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-connecting-securely: manual/connecting_securely.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-fetch-topics: manual/fetch_topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-messaging: manual/messaging.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-missing-topic-notification: manual/missing_topic_notification.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-reconnecting: manual/reconnecting.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-reconnection-strategy: manual/reconnection_strategy.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-remove-topics: manual/remove_topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-security-control: manual/security_control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-session-locks: manual/session_locks.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-session-properties: manual/session_properties.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-subscribe-to-json: manual/subscribe_to_json.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-subscription: manual/subscription.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-subscription-control: manual/subscription_control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-subscription-control-filter: manual/subscription_control_filter.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-system-authentication-control: manual/system_authentication_control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-timeseries-publish: manual/timeseries_publish.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-timeseries-subscribe: manual/timeseries_subscribe.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-topic-control: manual/topic_control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-topic-control-create-and-subscribe-json: manual/topic_control_create_and_subscribe_json.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-topic-notifications: manual/topic_notifications.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-topic-update-exclusive: manual/topic_update_exclusive.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-topic-update-json: manual/topic_update_json.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-topic-update-non-exclusive: manual/topic_update_non_exclusive.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

manual-value-streams: manual/value_streams.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@


clean:
		rm -rf $(TARGETS) $(OBJECTS) $(TARGETDIR) core a.out
