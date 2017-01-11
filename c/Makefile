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

CFLAGS		+= -g -Wall -Werror -std=c99 -D_POSIX_C_SOURCE=200112L -D_XOPEN_SOURCE=700 -c -I$(DIFFUSION_C_CLIENT_INCDIR)
LDFLAGS		+= $(LIBS) $(DIFFUSION_C_CLIENT_LIBDIR)/libdiffusion.a -lpthread -lpcre
# If you have openssl installed then you can uncomment these.
ifdef HAVE_OPEN_SSL
LDFLAGS     += -lssl -lcrypto
endif

ARFLAGS		+=
SOURCES = subscribe.c fetch.c rc-state.c rc-fortune.c fortune-client.c	\
		  send-msg.c send-msg-to-session.c send-msg-to-filter.c	\
		  auth-service.c					\
		  change-principal.c add-topics.c update-topic.c	\
		  subscribe-multiple.c update-record.c connect.c	\
		  connect-async.c system-auth-control.c                 \
		  msg-handler.c msg-listener.c				\
		  session-properties-listener.c                         \
		  get-session-properties.c                              \
	 	  missing-topic-notification.c				\
		  subscription-control.c reconnect.c

TARGETDIR	= target
OBJDIR		= $(TARGETDIR)/objs
BINDIR		= $(TARGETDIR)/bin
OBJECTS		= $(SOURCES:.c=.o)
TARGETS = subscribe fetch rc-state rc-fortune fortune-client            \
		  send-msg send-msg-to-session send-msg-to-filter	\
		  auth-service change-principal add-topics		\
		  update-topic subscribe-multiple update-record		\
		  connect connect-async system-auth-control             \
		  msg-handler msg-listener				\
		  session-properties-listener				\
		  get-session-properties                                \
                  missing-topic-notification				\
		  subscription-control reconnect

all:		prepare $(TARGETS)
.PHONY:		all

prepare:
		mkdir -p $(OBJDIR) $(BINDIR)

$(OBJDIR)/%.o:	%.c
		$(CC) $(CFLAGS) -o $@ $<

subscribe:	$(OBJDIR)/subscribe.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

fetch:		$(OBJDIR)/fetch.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

rc-state:	$(OBJDIR)/rc-state.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

rc-fortune:	$(OBJDIR)/rc-fortune.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

fortune-client:	$(OBJDIR)/fortune-client.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

send-msg:	$(OBJDIR)/send-msg.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

send-msg-to-session:	$(OBJDIR)/send-msg-to-session.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

send-msg-to-filter:	$(OBJDIR)/send-msg-to-filter.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

auth-service:	$(OBJDIR)/auth-service.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

change-principal:   $(OBJDIR)/change-principal.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

add-topics:	$(OBJDIR)/add-topics.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

update-topic:	$(OBJDIR)/update-topic.o
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

msg-handler:	$(OBJDIR)/msg-handler.o
		$(CC) $< $(LDFLAGS) -o $(BINDIR)/$@

msg-listener:	$(OBJDIR)/msg-listener.o
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

clean:
		rm -rf $(TARGETS) $(OBJECTS) $(TARGETDIR) core a.out
