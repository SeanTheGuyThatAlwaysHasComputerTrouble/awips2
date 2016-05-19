/*
 * edexBridge.cpp
 *
 *  Created on: Oct 8, 2009
 *      Author: brockwoo
 *  Updated on: June 21, 2013 (Re-written to work with the qpid messaging api)
 *      Author: bkowal    
 *  Updated on: May 06, 2014 (Issue #3102: Updated to call cleanup if connect failed.  Limit number of messages to be sent to QPID on a single send call)
 *      Author: rjpeter
 *  Updated on: Aug 05, 2014 (Omaha #3458: Added logging of error when issue occurs on send)
 *      Author: rjpeter
 *  Updated on: Nov 04, 2014 (Omaha #2991 Updated to work with QPID 0.30)
 *      Author: dlovely    
 */

#include <qpid/messaging/Connection.h>
#include <qpid/messaging/Session.h>
#include <qpid/messaging/Message.h>
#include <qpid/messaging/Sender.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/sem.h>
#include <sys/shm.h>
#include <stdlib.h>
#include <iostream>
#include <sstream>
#include <memory>
#include <filel.h>
#include <ulog.h>
#include <getopt.h>
#include <signal.h>
#include <sys/time.h>
#include <list>

using namespace qpid::messaging;
using namespace std;

class LdmProducer {
private:

	Connection connection;
	Session session;
        Sender sender;
	bool useTopic;
	bool sessionTransacted;
	bool isConnected;
	std::string brokerURI;
	int portNumber;
        std::string username;
        std::string password;
	list<string> filenameList;
	list<string> headerList;
    int maxMessagesPerSend;

public:

	LdmProducer(const std::string& brokerURI, int port = 5672,
                    const std::string& username = "guest",
                    const std::string& password = "guest",
                    bool useTopic = false, bool sessionTransacted = false,
                    int maxMessagesPerSend = 1000) 
        {
		this->useTopic = useTopic;
		this->sessionTransacted = sessionTransacted;
		this->brokerURI = brokerURI;
		this->isConnected = false;
		this->portNumber = port;
        this->username = username;
        this->password = password;
        this->maxMessagesPerSend = maxMessagesPerSend;
	}

	~LdmProducer() {
		cleanup();
	}

	void setMessages(int start, int size, edex_message * messages) {
		for (int i = 0; i < size - start; i++) {
			string fn = string(messages[start + i].filename);
			string hd = string(string(messages[start + i].ident));
			this->filenameList.push_back(fn);
			this->headerList.push_back(hd);
		}
	}

	int getMessageCount() {
		return this->filenameList.size();
	}

	int send() {
		int messagesProcessed = 0;
		if (!connect()) {
			return -1;
		}
		string fileLocation;
		string fileHeader;

		try {
            // limit number of messages sent at one time so we don't miss messages from shared memory if
            // a message build up occurred due to qpid being down (usual rate is under 100 a second)
			while ((!this->filenameList.empty()) && (messagesProcessed < this->maxMessagesPerSend)) {
				Message message;
                                
				fileLocation = this->filenameList.front();
				fileHeader = this->headerList.front();
				struct timeval tv;
				gettimeofday(&tv, NULL);
				uint64_t current = (((long long) tv.tv_sec) * 1000000
						+ ((long long) tv.tv_usec)) / 1000;
                                message.setDurable(true);
				message.setSubject(fileHeader);
				message.setContent(fileLocation);
				message.setProperty("enqueueTime", current);
                               
                                this->sender.send(message);

				this->filenameList.pop_front();
				this->headerList.pop_front();
				messagesProcessed++;
			}
		} catch (const std::exception& error) {
			// Error occurred during communication.  Clean up the connection and return the number of messages processed.
			uerror(error.what());
			cleanup();

		}
		return messagesProcessed;
	}

private:

	void cleanup()
	{
		unotice ("Cleaning up");

		// Destroy resources.
		if (this->sender != 0)
		{
			try
			{
				this->sender.close();
				this->sender = 0;
			}
			catch (const std::exception& error)
			{
				uwarn(error.what());
			}
		}

		if (this->session != 0)
		{
			try
			{
				this->session.close();
				this->session = 0;
			}
			catch (const std::exception& error)
			{
				uwarn(error.what());
			}
		}

		if (this->connection != 0)
		{
			try
			{
				this->connection.close();
				this->connection = 0;
			}
			catch (const std::exception& error)
			{
				uwarn(error.what());
			}
        }
		this->isConnected = false;
	}

	bool connect()
	{
		if (this->isConnected)
		{
			return this->isConnected;
		}
		try
		{
			// initialize
			this->connection = 0;
			this->session = 0;
			this->sender = 0;

            std::stringstream qpidURLBuilder;
            qpidURLBuilder << "amqp:tcp:";
            qpidURLBuilder << this->brokerURI;
            qpidURLBuilder << ":";
            qpidURLBuilder << this->portNumber;
            std::string qpidURL = qpidURLBuilder.str();

            std::stringstream connectionOptionsBuilder;
            connectionOptionsBuilder << "{sasl-mechanism:ANONYMOUS,username:";
            connectionOptionsBuilder << this->username;
            connectionOptionsBuilder << ",password:";
            connectionOptionsBuilder << this->password;
            connectionOptionsBuilder << "}";
            std::string connectionOptions = connectionOptionsBuilder.str();

            this->connection = Connection(qpidURL, connectionOptions);
			this->connection.open();

            std::string address = "external.dropbox; {node:{type:queue,durable:true,x-bindings:"
            		"[{exchange:amq.direct,queue:external.dropbox,key:external.dropbox}]}}";

            this->session = this->connection.createSession();
            this->sender = this->session.createSender(address);
                        
			this->isConnected = true;
		}
		catch (const std::exception& error)
		{
            uerror(error.what());
			cleanup();
		}
		return this->isConnected;
	}

};

static volatile int hupped = 0;
static volatile int done = 0;
static edex_message * messageCursor;

static void cleanup(void) {
	unotice("Exiting");
	(void) closeulog();
}

/*
 * called upon receipt of signals
 */
static void signal_handler(int sig) {
#ifdef SVR3SIGNALS
	/*
	 * Some systems reset handler to SIG_DFL upon entry to handler.
	 * In that case, we reregister our handler.
	 */
	(void) signal(sig, signal_handler);
#endif
	switch (sig) {
	case SIGHUP:
		hupped = 1;
		return;
	case SIGINT:
		exit(0);
		/*NOTREACHED*/
	case SIGTERM:
		done = 1;
		return;
	case SIGUSR1:
		/* TODO? stats */
		return;
	case SIGUSR2:
		rollulogpri();
		return;
	case SIGALRM:
		return;
	}
}

/*
 * register the signal_handler
 */
static void set_sigactions(void) {
	struct sigaction sigact;

	sigemptyset(&sigact.sa_mask);
	sigact.sa_flags = 0;

	/* Ignore these */
	sigact.sa_handler = SIG_IGN;
	(void) sigaction(SIGPIPE, &sigact, NULL);

	/* Handle these */
#ifdef SA_RESTART       /* SVR4, 4.3+ BSD */
	/* usually, restart system calls */
	sigact.sa_flags |= SA_RESTART;
	/*
	 * NOTE: The OSF/1 operating system doesn't conform to the UNIX standard
	 * in this regard: the SA_RESTART flag does not affect writes to regular
	 * files or, apparently, pipes.  Consequently, interrupted writes must
	 * be handled explicitly.  See the discussion of the SA_RESTART option
	 * at http://www.opengroup.org/onlinepubs/007908799/xsh/sigaction.html
	 */
#endif
	sigact.sa_handler = signal_handler;
	(void) sigaction(SIGHUP, &sigact, NULL);
	(void) sigaction(SIGTERM, &sigact, NULL);
	(void) sigaction(SIGUSR1, &sigact, NULL);
	(void) sigaction(SIGUSR2, &sigact, NULL);
	(void) sigaction(SIGALRM, &sigact, NULL);

	/* Don't restart after interrupt */
	sigact.sa_flags = 0;
#ifdef SA_INTERRUPT     /* SunOS 4.x */
	sigact.sa_flags |= SA_INTERRUPT;
#endif
	(void) sigaction(SIGINT, &sigact, NULL);
}

int main(int argc, char* argv[]) {

	char * logfname = 0;
	int loggingToStdErr = 0;
	std::string brokerURI = "127.0.0.1";
	int port = 5672;
        std::string username = "guest";
        std::string password = "guest";

	{
		extern char *optarg;

		int ch;
		int logmask = (LOG_MASK(LOG_ERR) | LOG_MASK(LOG_WARNING)
				| LOG_MASK(LOG_NOTICE));

		while ((ch = getopt(argc, argv, "vxl:s:p:")) != EOF) {
			switch (ch) {
			case 'v':
				logmask |= LOG_UPTO(LOG_INFO);
				break;
			case 'x':
				logmask |= LOG_MASK(LOG_DEBUG);
				break;
			case 'l':
				logfname = optarg;
				break;
			case 's':
				brokerURI = string(optarg);
				break;
			case 'p':
				port = atoi(optarg);
				break;

			}
		}
		(void) setulogmask(logmask);
	}

	if (atexit(cleanup) != 0) {
		serror("atexit");
		unotice("Exiting");
		exit(1);
		/*NOTREACHED*/
	}

	loggingToStdErr = STDERR_FILENO == openulog(ubasename(argv[0]), (LOG_CONS
			| LOG_PID), LOG_LDM, logfname);
	unotice("Starting Up");

	set_sigactions();

	//============================================================
	// set to true to use topics instead of queues
	// Note in the code above that this causes createTopic or
	// createQueue to be used in both consumer an producer.
	//============================================================
	bool useTopics = false;
    int maxMessagesPerSend = 1000;
    bool sessionTransacted = false;

	int shmid;
	int semid;
	key_t key = ftok("/etc/rc.d/rc.local", 'R');
	key_t semkey = ftok("/etc/rc.d/rc.local", 'e');
	int lastQueueSize = 0;
	semid = semget(semkey, 2, 0666);
	int semCounter = 0;
	while ((semid = semget(semkey, 2, 0666)) == -1) {
		if (semCounter == 5) {
			uerror(
					"Could not attach to the semaphore created by pqact.  Exiting.");
			exit(0);
		}
		semCounter++;
		sleep(1);
	}
	int sizeOfQueue = semctl(semid, 0, GETVAL);
	shmid = shmget(key, sizeof(edex_message) * sizeOfQueue, 0666);
	if (shmid == -1) {
		uerror(
				"Could not attach to the shared memory created by pqact.  Exiting.");
		exit(0);
	}

	messageCursor = (edex_message *) shmat(shmid, (void *) 0, 0);

	LdmProducer producer(brokerURI, port, username, password, useTopics, sessionTransacted, maxMessagesPerSend);

	for (;;) {
		if (hupped) {
			// Should reset connections to shared memory here
			hupped = 0;
		}
		if (done) {
			break;
		}
		int presentQueueLocation = semctl(semid, 1, GETVAL);
		if (presentQueueLocation < 0) { // Nothing has been put on the queue yet
			sleep(1);
			continue;
		}
		int queueSize = presentQueueLocation + 1;
		if (lastQueueSize != queueSize) {
			int endQueueDiff = 0;

			// Need to copy in the end of the queue before moving to front
			if (lastQueueSize > queueSize) {
				unotice(
						"Coming over the top with lastQueueSize of %d on a size of %d",
						lastQueueSize, sizeOfQueue);
				endQueueDiff = sizeOfQueue - lastQueueSize;
				if (endQueueDiff > 0) {
					producer.setMessages(lastQueueSize, sizeOfQueue,
							messageCursor);
				}

				lastQueueSize = 0;
			}
			int queue_diff = queueSize - lastQueueSize;
			if (queue_diff > 0) {
				producer.setMessages(lastQueueSize, queueSize, messageCursor);
			}

			lastQueueSize = queueSize;

			int messagesSent = 0;
			if ((messagesSent = producer.send()) == -1) {
				uerror(
						"Could not connect to the remote EDEX instance.  %d messages waiting to be sent.  Will try again in 1 second.",
						producer.getMessageCount());
				sleep(1);
				continue;
			}
			if ((messagesSent != maxMessagesPerSend) && (messagesSent != (queue_diff + endQueueDiff))) {
				uerror(
						"Only %d messages were sent out of an expected %d.  Will store those not sent and try again.",
						messagesSent, queue_diff);

			}
			unotice(
					"Sent %d messages (%d at the end of the queue, %d normally).",
					messagesSent, endQueueDiff, queue_diff);
		}
		sleep(1);
	}
	shmdt(messageCursor);
	exit(0);
}

