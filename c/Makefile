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

CC		= gcc

# Extra definitions from parent directory, if they exist.
-include ../makefile.defs

CFLAGS		+= -g -std=c99 -D_POSIX_C_SOURCE=200112L -D_XOPEN_SOURCE=700 \
			   -Wall -Werror -Wno-error=deprecated-declarations \
			   -I$(DIFFUSION_C_CLIENT_INCDIR)
LDFLAGS		+= $(DIFFUSION_C_CLIENT_LIBDIR)/libdiffusion.a -lpthread -lpcre -lz $(LIBS)

ARFLAGS		+=
SOURCES 	=	subscribe.c \
				fetch-request.c \
				auth-service.c \
				change-principal.c \
				add-topics.c \
				subscribe-multiple.c \
				update-record.c \
				connect.c \
				connect-async.c \
				system-auth-control.c \
				session-properties-listener.c \
				get-session-properties.c \
				missing-topic-notification.c \
				subscription-control.c \
				reconnect.c \
				json/json-publishing-example.c \
				json/json-subscribing-example.c \
				recordv2-topics.c \
				binary-topics.c \
				int64-topics.c \
				double-topics.c \
				string-topics.c \
				send-request-to-session.c \
				send-request-to-path.c \
				send-request-to-filter.c \
				topic-update.c \
				topic-update-with-constraint.c \
				topic-update-stream.c \
				topic-update-add-and-set.c \
				time-series-append.c	\
				topic-views.c \
				time-series-edit.c \
				time-series-range-query.c \
				topic-views-list.c \
				topic-views-remove.c \
				time-series-timestamp-append.c

TARGETDIR	= 	target
OBJDIR		= 	$(TARGETDIR)/objs
BINDIR		= 	$(TARGETDIR)/bin
OBJECTS		= 	$(SOURCES:.c=.o)
TARGETS 	=	subscribe \
				fetch-request \
				auth-service \
				change-principal \
				add-topics \
				subscribe-multiple \
				update-record \
				connect \
				connect-async \
				system-auth-control \
				session-properties-listener \
				get-session-properties \
				missing-topic-notification \
				subscription-control \
				reconnect \
				json-publishing-example \
				json-subscribing-example \
				recordv2-topics \
				binary-topics \
				int64-topics \
				double-topics \
				string-topics \
				send-request-to-session \
				send-request-to-path \
				send-request-to-filter \
				topic-update \
				topic-update-with-constraint \
				topic-update-stream \
				topic-update-add-and-set \
				time-series-append \
				topic-views \
				time-series-edit \
				time-series-range-query \
				topic-views-list \
				topic-views-remove \
				time-series-timestamp-append

all:		prepare $(TARGETS)
.PHONY:		all

prepare:
		mkdir -p $(OBJDIR) $(BINDIR)

$(OBJDIR)/%.o:	%.c
		$(CC) $(CFLAGS) -c -o $@ $<

subscribe:	$(OBJDIR)/subscribe.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

fetch-request:		$(OBJDIR)/fetch-request.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

send-request-to-session:	$(OBJDIR)/send-request-to-session.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

send-request-to-path:	$(OBJDIR)/send-request-to-path.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

send-request-to-filter:	$(OBJDIR)/send-request-to-filter.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

auth-service:	$(OBJDIR)/auth-service.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

change-principal:   $(OBJDIR)/change-principal.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

add-topics:	$(OBJDIR)/add-topics.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

update-record:	$(OBJDIR)/update-record.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

subscribe-multiple:	$(OBJDIR)/subscribe-multiple.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

connect:	$(OBJDIR)/connect.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

connect-async:	$(OBJDIR)/connect-async.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

system-auth-control:	$(OBJDIR)/system-auth-control.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

session-properties-listener:	$(OBJDIR)/session-properties-listener.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

get-session-properties:	$(OBJDIR)/get-session-properties.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

missing-topic-notification:	$(OBJDIR)/missing-topic-notification.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

subscription-control:	$(OBJDIR)/subscription-control.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

reconnect:	$(OBJDIR)/reconnect.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

json-publishing-example:	json/json-publishing-example.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

json-subscribing-example:	json/json-subscribing-example.c
		$(CC) $^ $(CFLAGS) $(LDFLAGS) -lm -o $(BINDIR)/$@

recordv2-topics:	$(OBJDIR)/recordv2-topics.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

binary-topics:	$(OBJDIR)/binary-topics.o

		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

int64-topics:	$(OBJDIR)/int64-topics.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

double-topics:	$(OBJDIR)/double-topics.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

string-topics:	$(OBJDIR)/string-topics.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

topic-update:	$(OBJDIR)/topic-update.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

topic-update-with-constraint:	$(OBJDIR)/topic-update-with-constraint.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

topic-update-stream:	$(OBJDIR)/topic-update-stream.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

topic-update-add-and-set:	$(OBJDIR)/topic-update-add-and-set.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

time-series-append:	$(OBJDIR)/time-series-append.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

time-series-timestamp-append:	$(OBJDIR)/time-series-timestamp-append.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

time-series-edit:	$(OBJDIR)/time-series-edit.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

time-series-range-query:	$(OBJDIR)/time-series-range-query.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

topic-views:	$(OBJDIR)/topic-views.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

topic-views-list:	$(OBJDIR)/topic-views-list.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

topic-views-remove:	$(OBJDIR)/topic-views-remove.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@


clean:
		rm -rf $(TARGETS) $(OBJECTS) $(TARGETDIR) core a.out
