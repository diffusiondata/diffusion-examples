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

CC		= gcc

# Extra definitions from parent directory, if they exist.
-include makefile.defs

CFLAGS		+= -g -Wall -Werror -Wno-error=deprecated-declarations -std=c99 -D_POSIX_C_SOURCE=200112L -D_XOPEN_SOURCE=700 -I$(DIFFUSION_C_CLIENT_INCDIR)
LDFLAGS		+= $(DIFFUSION_C_CLIENT_LIBDIR)/libdiffusion.a -lpthread -lpcre -lz $(LIBS)

# If you have openssl installed then you can uncomment these.
ifdef HAVE_OPEN_SSL
LDFLAGS     += -lssl -lcrypto
endif

ARFLAGS		+=
SOURCES 	= connect-async.c \
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

OBJDIR		= $(TARGETDIR)/objs
BINDIR		= $(TARGETDIR)/bin
OBJECTS		= $(SOURCES:.c=.o)
TARGETS 	= connect-async \
			  connect \
			  reconnect \
			  authentication_control \
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

all:		prepare $(TARGETS)
.PHONY:		all

prepare:
		mkdir -p $(OBJDIR) $(BINDIR)

$(OBJDIR)/%.o:	%.c
		$(CC) $(CFLAGS) -c -o $@ $<

connect-async:	$(OBJDIR)/connect-async.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

connect:	$(OBJDIR)/connect.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

reconnect:	$(OBJDIR)/reconnect.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

authentication_control:	features/authentication_control/auth-service.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-change-roles-with-filter:	features/client_control/change-roles-with-filter.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-change-roles-with-session:	features/client_control/change-roles-with-session.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-close-with-filter:	features/client_control/close-with-filter.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-close-with-session:	features/client_control/close-with-session.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-get-session-properties:	features/client_control/get-session-properties.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

client-control-session-properties-listener:	features/client_control/session-properties-listener.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

messaging-send-request-to-filter:	features/messaging/send-request-to-filter.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

messaging-send-request-to-path:	features/messaging/send-request-to-path.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

messaging-send-request-to-session:	features/messaging/send-request-to-session.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

metrics-session-metric-collector: features/metrics/session-metric-collector.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

metrics-topic-metric-collector: features/metrics/topic-metric-collector.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

remote-servers: features/remote_servers/remote-servers.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

session-trees:	features/session_trees/session-trees.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

security-change-principal:	features/security/change-principal.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

subscription-control:	features/subscription_control/subscription-control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

system-authentication-control:	features/system_authentication_control/system-auth-control.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

time-series-timestamp-append:	features/time_series/time-series-timestamp-append.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

time-series-range-query:	features/time_series/time-series-range-query.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

time-series-append:	features/time_series/time-series-append.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

time-series-edit:	features/time_series/time-series-edit.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-control-missing-topic-notification:	features/topic_control/missing-topic-notification.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-control-add-topics:	features/topic_control/add-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update-record:	features/topic_update/update-record.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update:	features/topic_update/topic-update.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update-stream:	features/topic_update/topic-update-stream.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update-with-constraint:	features/topic_update/topic-update-with-constraint.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-update-add-and-set:	features/topic_update/topic-update-add-and-set.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-views:	features/topic_views/topic-views.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-views-remove:	features/topic_views/topic-views-remove.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topic-views-list:	features/topic_views/topic-views-list.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-subscribe:	features/topics/subscribe.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-subscribe-multiple:	features/topics/subscribe-multiple.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-recordv2:	features/topics/recordv2-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-string:	features/topics/string-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-double:	features/topics/double-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-fetch:	features/topics/fetch-request.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-int64:	features/topics/int64-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

topics-binary:	features/topics/binary-topics.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

clean:
		rm -rf $(TARGETS) $(OBJECTS) $(TARGETDIR) core a.out
