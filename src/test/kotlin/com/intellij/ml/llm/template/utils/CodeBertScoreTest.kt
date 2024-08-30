package com.intellij.ml.llm.template.utils

import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class CodeBertScoreTest{

    @Test
    fun testCodeBertScore(){
//        {"text1": "public void example123js() { }", "text2": "public void sample() { }"}
        println (CodeBertScore.computeCodeBertScore("public void example123js() { }", "public void sample() { }"))

    }

    @Test
    fun testCodeBertScore2(){
    val text1 = "/**\n" +
            "    * If we are within one transaction we won't do any replication as replication would only be performed at commit\n" +
            "    * time. If the operation didn't originate locally we won't do any replication either.\n" +
            "    */\n" +
            "   private Object handleTxWriteCommand(InvocationContext ctx, WriteCommand command, Object recipientGenerator, boolean skipRemoteGet) throws Throwable {\n" +
            "      // see if we need to load values from remote sources first\n" +
            "      if (!skipRemoteGet && needValuesFromPreviousOwners(ctx, command))\n" +
            "         remoteGetBeforeWrite(ctx, command, recipientGenerator);\n" +
            "\n" +
            "      // FIRST pass this call up the chain.  Only if it succeeds (no exceptions) locally do we attempt to distribute.\n" +
            "      return invokeNextInterceptor(ctx, command);\n" +
            "   }"
        val text2 = "/**\n" +
                " * Infinispan's log abstraction layer on top of JBoss Logging.\n" +
                " * <p/>\n" +
                " * It contains explicit methods for all INFO or above levels so that they can\n" +
                " * be internationalized. For the core module, message ids ranging from 0001\n" +
                " * to 0900 inclusively have been reserved.\n" +
                " * <p/>\n" +
                " * <code> Log log = LogFactory.getLog( getClass() ); </code> The above will get\n" +
                " * you an instance of <tt>Log</tt>, which can be used to generate log messages\n" +
                " * either via JBoss Logging which then can delegate to Log4J (if the libraries\n" +
                " * are present) or (if not) the built-in JDK logger.\n" +
                " * <p/>\n" +
                " * In addition to the 6 log levels available, this framework also supports\n" +
                " * parameter interpolation, similar to the JDKs {@link String#format(String, Object...)}\n" +
                " * method. What this means is, that the following block:\n" +
                " * <code> if (log.isTraceEnabled()) { log.trace(\"This is a message \" + message + \" and some other value is \" + value); }\n" +
                " * </code>\n" +
                " * <p/>\n" +
                " * ... could be replaced with ...\n" +
                " * <p/>\n" +
                " * <code> if (log.isTraceEnabled()) log.tracef(\"This is a message %s and some other value is %s\", message, value);\n" +
                " * </code>\n" +
                " * <p/>\n" +
                " * This greatly enhances code readability.\n" +
                " * <p/>\n" +
                " * If you are passing a <tt>Throwable</tt>, note that this should be passed in\n" +
                " * <i>before</i> the vararg parameter list.\n" +
                " * <p/>\n" +
                " *\n" +
                " * @author Manik Surtani\n" +
                " * @since 4.0\n" +
                " * @private\n" +
                " */\n" +
                "@MessageLogger(projectCode = \"ISPN\")\n" +
                "public interface Log extends BasicLogger {\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to load %s from cache loader\", id = 1)\n" +
                "   void unableToLoadFromCacheLoader(Object key, @Cause PersistenceException cle);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Field %s not found!!\", id = 2)\n" +
                "   void fieldNotFound(String fieldName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Property %s could not be replaced as intended!\", id = 3)\n" +
                "   void propertyCouldNotBeReplaced(String line);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unexpected error reading properties\", id = 4)\n" +
                "   void errorReadingProperties(@Cause IOException e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Detected write skew on key [%s]. Another process has changed the entry since we last read it! Unable to copy entry for update.\", id = 5)\n" +
                "   void unableToCopyEntryForUpdate(Object key);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Failed remote execution on node %s\", id = 6)\n" +
                "   void remoteExecutionFailed(Address address, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Failed local execution \", id = 7)\n" +
                "   void localExecutionFailed(@Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Can not select %s random members for %s\", id = 8)\n" +
                "   void cannotSelectRandomMembers(int numNeeded, List<Address> members);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"DistributionManager not yet joined the cluster. Cannot do anything about other concurrent joiners.\", id = 14)\n" +
                "   void distributionManagerNotJoined();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"DistributionManager not started after waiting up to 5 minutes! Not rehashing!\", id = 15)\n" +
                "   void distributionManagerNotStarted();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Problem %s encountered when applying state for key %s!\", id = 16)\n" +
                "   void problemApplyingStateForKey(String msg, Object key, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to apply prepare %s\", id = 18)\n" +
                "   void unableToApplyPrepare(PrepareCommand pc, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Couldn't acquire shared lock\", id = 19)\n" +
                "   void couldNotAcquireSharedLock();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Expected just one response; got %s\", id = 21)\n" +
                "   void expectedJustOneResponse(Map<Address, Response> lr);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"wakeUpInterval is <= 0, not starting expired purge thread\", id = 25)\n" +
                "   void notStartingEvictionThread();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Caught exception purging data container!\", id = 26)\n" +
                "   void exceptionPurgingDataContainer(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not acquire lock for eviction of %s\", id = 27)\n" +
                "   void couldNotAcquireLockForEviction(Object key, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to passivate entry under %s\", id = 28)\n" +
                "   void unableToPassivateEntry(Object key, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Passivating all entries to disk\", id = 29)\n" +
                "   void passivatingAllEntries();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Passivated %d entries in %s\", id = 30)\n" +
                "   void passivatedEntries(int numEntries, String duration);\n" +
                "\n" +
                "   @LogMessage(level = TRACE)\n" +
                "   @Message(value = \"MBeans were successfully registered to the platform MBean server.\", id = 31)\n" +
                "   void mbeansSuccessfullyRegistered();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Problems un-registering MBeans\", id = 32)\n" +
                "   void problemsUnregisteringMBeans(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to unregister Cache MBeans with pattern %s\", id = 33)\n" +
                "   void unableToUnregisterMBeanWithPattern(String pattern, @Cause MBeanRegistrationException e);\n" +
                "\n" +
                "   @Message(value = \"There's already a JMX MBean instance %s already registered under \" +\n" +
                "         \"'%s' JMX domain. If you want to allow multiple instances configured \" +\n" +
                "         \"with same JMX domain enable 'allowDuplicateDomains' attribute in \" +\n" +
                "         \"'globalJmxStatistics' config element\", id = 34)\n" +
                "   JmxDomainConflictException jmxMBeanAlreadyRegistered(String mBeanName, String jmxDomain);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not reflect field description of this class. Was it removed?\", id = 35)\n" +
                "   void couldNotFindDescriptionField();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Did not find attribute %s\", id = 36)\n" +
                "   void couldNotFindAttribute(String name);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Failed to update attribute name %s with value %s\", id = 37)\n" +
                "   void failedToUpdateAttribute(String name, Object value);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Method name %s doesn't start with \\\"get\\\", \\\"set\\\", or \\\"is\\\" \" +\n" +
                "         \"but is annotated with @ManagedAttribute: will be ignored\", id = 38)\n" +
                "   void ignoringManagedAttribute(String methodName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Method %s must have a valid return type and zero parameters\", id = 39)\n" +
                "   void invalidManagedAttributeMethod(String methodName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Not adding annotated method %s since we already have read attribute\", id = 40)\n" +
                "   void readManagedAttributeAlreadyPresent(Method m);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Not adding annotated method %s since we already have writable attribute\", id = 41)\n" +
                "   void writeManagedAttributeAlreadyPresent(String methodName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Did not find queried attribute with name %s\", id = 42)\n" +
                "   void queriedAttributeNotFound(String attributeName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Exception while writing value for attribute %s\", id = 43)\n" +
                "   void errorWritingValueForAttribute(String attributeName, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not invoke set on attribute %s with value %s\", id = 44)\n" +
                "   void couldNotInvokeSetOnAttribute(String attributeName, Object value);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Problems encountered while purging expired\", id = 45)\n" +
                "   void problemPurgingExpired(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unknown responses from remote cache: %s\", id = 46)\n" +
                "   void unknownResponsesFromRemoteCache(Collection<Response> responses);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error while doing remote call\", id = 47)\n" +
                "   void errorDoingRemoteCall(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Interrupted or timeout while waiting for AsyncCacheWriter worker threads to push all state to the decorated store\", id = 48)\n" +
                "   void interruptedWaitingAsyncStorePush(@Cause InterruptedException e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unexpected error\", id = 51)\n" +
                "   void unexpectedErrorInAsyncProcessor(@Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Interrupted on acquireLock for %d milliseconds!\", id = 52)\n" +
                "   void interruptedAcquiringLock(long ms, @Cause InterruptedException e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to process some async modifications after %d retries!\", id = 53)\n" +
                "   void unableToProcessAsyncModifications(int retries);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"AsyncStoreCoordinator interrupted\", id = 54)\n" +
                "   void asyncStoreCoordinatorInterrupted(@Cause InterruptedException e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unexpected error in AsyncStoreCoordinator thread. AsyncCacheWriter is dead!\", id = 55)\n" +
                "   void unexpectedErrorInAsyncStoreCoordinator(@Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Exception reported changing cache active status\", id = 58)\n" +
                "   void errorChangingSingletonStoreStatus(@Cause SingletonCacheWriter.PushStateException e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Had problems removing file %s\", id = 59)\n" +
                "   void problemsRemovingFile(File f);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Problems purging file %s\", id = 60)\n" +
                "   void problemsPurgingFile(File buckedFile, @Cause PersistenceException e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to acquire global lock to purge cache store\", id = 61)\n" +
                "   void unableToAcquireLockToPurgeStore();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error while reading from file: %s\", id = 62)\n" +
                "   void errorReadingFromFile(File f, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Problems creating the directory: %s\", id = 64)\n" +
                "   void problemsCreatingDirectory(File dir);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Exception while marshalling object: %s\", id = 65)\n" +
                "   void errorMarshallingObject(@Cause IOException ioe, Object obj);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unable to read version id from first two bytes of stream, barfing.\", id = 66)\n" +
                "   void unableToReadVersionId();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Will try and wait for the cache %s to start\", id = 67)\n" +
                "   void waitForCacheToStart(String cacheName);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Cache named %s does not exist on this cache manager!\", id = 68)\n" +
                "   void namedCacheDoesNotExist(String cacheName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Caught exception when handling command %s\", id = 71)\n" +
                "   void exceptionHandlingCommand(ReplicableCommand cmd, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Failed replicating %d elements in replication queue\", id = 72)\n" +
                "   void failedReplicatingQueue(int size, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unexpected error while replicating\", id = 73)\n" +
                "   void unexpectedErrorReplicating(@Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Message or message buffer is null or empty.\", id = 77)\n" +
                "   void msgOrMsgBufferEmpty();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Starting JGroups channel %s\", id = 78)\n" +
                "   void startingJGroupsChannel(String cluster);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Channel %s local address is %s, physical addresses are %s\", id = 79)\n" +
                "   void localAndPhysicalAddress(String cluster, Address address, List<Address> physicalAddresses);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Disconnecting JGroups channel %s\", id = 80)\n" +
                "   void disconnectJGroups(String cluster);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Problem closing channel %s; setting it to null\", id = 81)\n" +
                "   void problemClosingChannel(@Cause Exception e, String cluster);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Stopping the RpcDispatcher for channel %s\", id = 82)\n" +
                "   void stoppingRpcDispatcher(String cluster);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Class [%s] cannot be cast to JGroupsChannelLookup! Not using a channel lookup.\", id = 83)\n" +
                "   void wrongTypeForJGroupsChannelLookup(String channelLookupClassName, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Errors instantiating [%s]! Not using a channel lookup.\", id = 84)\n" +
                "   void errorInstantiatingJGroupsChannelLookup(String channelLookupClassName, @Cause Exception e);\n" +
                "\n" +
                "   @Message(value = \"Error while trying to create a channel using the specified configuration file: %s\", id = 85)\n" +
                "   CacheConfigurationException errorCreatingChannelFromConfigFile(String cfg, @Cause Exception e);\n" +
                "\n" +
                "   @Message(value = \"Error while trying to create a channel using the specified configuration XML: %s\", id = 86)\n" +
                "   CacheConfigurationException errorCreatingChannelFromXML(String cfg, @Cause Exception e);\n" +
                "\n" +
                "   @Message(value = \"Error while trying to create a channel using the specified configuration string: %s\", id = 87)\n" +
                "   CacheConfigurationException errorCreatingChannelFromConfigString(String cfg, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Unable to use any JGroups configuration mechanisms provided in properties %s. \" +\n" +
                "         \"Using default JGroups configuration!\", id = 88)\n" +
                "   void unableToUseJGroupsPropertiesProvided(TypedProperties props);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"getCoordinator(): Interrupted while waiting for members to be set\", id = 89)\n" +
                "   void interruptedWaitingForCoordinator(@Cause InterruptedException e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Channel not set up properly!\", id = 92)\n" +
                "   void channelNotSetUp();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Received new, MERGED cluster view for channel %s: %s\", id = 93)\n" +
                "   void receivedMergedView(String cluster, View newView);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Received new cluster view for channel %s: %s\", id = 94)\n" +
                "   void receivedClusterView(String cluster, View newView);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error while processing a prepare in a single-phase transaction\", id = 97)\n" +
                "   void errorProcessing1pcPrepareCommand(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Exception while rollback\", id = 98)\n" +
                "   void errorRollingBack(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unprocessed Transaction Log Entries! = %d\", id = 99)\n" +
                "   void unprocessedTxLogEntries(int size);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Stopping, but there are %s local transactions and %s remote transactions that did not finish in time.\", id = 100)\n" +
                "   void unfinishedTransactionsRemain(int localTransactions, int remoteTransactions);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Failed synchronization registration\", id = 101)\n" +
                "   void failedSynchronizationRegistration(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to roll back global transaction %s\", id = 102)\n" +
                "   void unableToRollbackGlobalTx(GlobalTransaction gtx, @Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"A remote transaction with the given id was already registered!!!\", id = 103)\n" +
                "   void remoteTxAlreadyRegistered();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Falling back to DummyTransactionManager from Infinispan\", id = 104)\n" +
                "   void fallingBackToDummyTm();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Failed creating initial JNDI context\", id = 105)\n" +
                "   void failedToCreateInitialCtx(@Cause NamingException e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Found WebSphere TransactionManager factory class [%s], but \" +\n" +
                "         \"couldn't invoke its static 'getTransactionManager' method\", id = 106)\n" +
                "   void unableToInvokeWebsphereStaticGetTmMethod(@Cause Exception ex, String className);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Retrieving transaction manager %s\", id = 107)\n" +
                "   void retrievingTm(TransactionManager tm);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error enlisting resource\", id = 108)\n" +
                "   void errorEnlistingResource(@Cause XAException e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"beforeCompletion() failed for %s\", id = 109)\n" +
                "   void beforeCompletionFailed(Synchronization s, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unexpected error from resource manager!\", id = 110)\n" +
                "   void unexpectedErrorFromResourceManager(@Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"afterCompletion() failed for %s\", id = 111)\n" +
                "   void afterCompletionFailed(Synchronization s, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"exception while committing\", id = 112)\n" +
                "   void errorCommittingTx(@Cause XAException e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unbinding of DummyTransactionManager failed\", id = 113)\n" +
                "   void unbindingDummyTmFailed(@Cause NamingException e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unsupported combination (dldEnabled, recoveryEnabled, xa) = (%s, %s, %s)\", id = 114)\n" +
                "   void unsupportedTransactionConfiguration(boolean dldEnabled, boolean recoveryEnabled, boolean xa);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Recovery call will be ignored as recovery is disabled. \" +\n" +
                "         \"More on recovery: http://community.jboss.org/docs/DOC-16646\", id = 115)\n" +
                "   void recoveryIgnored();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Missing the list of prepared transactions from node %s. \" +\n" +
                "         \"Received response is %s\", id = 116)\n" +
                "   void missingListPreparedTransactions(Object key, Object value);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"There's already a prepared transaction with this xid: %s. \" +\n" +
                "         \"New transaction is %s. Are there two different transactions having same Xid in the cluster?\", id = 117)\n" +
                "   void preparedTxAlreadyExists(RecoveryAwareTransaction previous,\n" +
                "                                RecoveryAwareRemoteTransaction remoteTransaction);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not load module at URL %s\", id = 118)\n" +
                "   void couldNotLoadModuleAtUrl(URL url, @Cause Exception ex);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Module %s loaded, but could not be initialized\", id = 119)\n" +
                "   void couldNotInitializeModule(Object key, @Cause Exception ex);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Invocation of %s threw an exception %s. Exception is ignored.\", id = 120)\n" +
                "   void ignoringException(String methodName, String exceptionName, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unable to set value!\", id = 121)\n" +
                "   void unableToSetValue(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to convert string property [%s] to an int! Using default value of %d\", id = 122)\n" +
                "   void unableToConvertStringPropertyToInt(String value, int defaultValue);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to convert string property [%s] to a long! Using default value of %d\", id = 123)\n" +
                "   void unableToConvertStringPropertyToLong(String value, long defaultValue);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to convert string property [%s] to a boolean! Using default value of %b\", id = 124)\n" +
                "   void unableToConvertStringPropertyToBoolean(String value, boolean defaultValue);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to invoke getter %s on Configuration.class!\", id = 125)\n" +
                "   void unableToInvokeGetterOnConfiguration(Method getter, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Attempted to stop() from FAILED state, but caught exception; try calling destroy()\", id = 126)\n" +
                "   void failedToCallStopAfterFailure(@Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Needed to call stop() before destroying but stop() threw exception. Proceeding to destroy\", id = 127)\n" +
                "   void stopBeforeDestroyFailed(@Cause CacheException e);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Infinispan version: %s\", id = 128)\n" +
                "   void version(String version);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Received a remote call but the cache is not in STARTED state - ignoring call.\", id = 129)\n" +
                "   void cacheNotStarted();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Caught exception! Aborting join.\", id = 130)\n" +
                "   void abortingJoin(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"%s completed join rehash in %s!\", id = 131)\n" +
                "   void joinRehashCompleted(Address self, String duration);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"%s aborted join rehash after %s!\", id = 132)\n" +
                "   void joinRehashAborted(Address self, String duration);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Attempted to register listener of class %s, but no valid, \" +\n" +
                "         \"public methods annotated with method-level event annotations found! \" +\n" +
                "         \"Ignoring listener.\", id = 133)\n" +
                "   void noAnnotateMethodsFoundInListener(Class<?> listenerClass);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to invoke method %s on Object instance %s - \" +\n" +
                "         \"removing this target object from list of listeners!\", id = 134)\n" +
                "   void unableToInvokeListenerMethodAndRemoveListener(Method m, Object target, @Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not lock key %s in order to invalidate from L1 at node %s, skipping....\", id = 135)\n" +
                "   void unableToLockToInvalidate(Object key, Address address);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Execution error\", id = 136)\n" +
                "   void executionError(@Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Failed invalidating remote cache\", id = 137)\n" +
                "   void failedInvalidatingRemoteCache(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Could not register object with name: %s\", id = 138)\n" +
                "   void couldNotRegisterObjectName(ObjectName objectName, @Cause InstanceAlreadyExistsException e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Infinispan configuration schema could not be resolved locally nor fetched from URL. Local path=%s, schema path=%s, schema URL=%s\", id = 139)\n" +
                "   void couldNotResolveConfigurationSchema(String localPath, String schemaPath, String schemaURL);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Lazy deserialization configuration is deprecated, please use storeAsBinary instead\", id = 140)\n" +
                "   void lazyDeserializationDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not rollback prepared 1PC transaction. This transaction will be rolled back by the recovery process, if enabled. Transaction: %s\", id = 141)\n" +
                "   void couldNotRollbackPrepared1PcTransaction(LocalTransaction localTransaction, @Cause Throwable e1);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"The async store shutdown timeout (%d ms) is too high compared \" +\n" +
                "         \"to cache stop timeout (%d ms), so instead using %d ms for async store stop wait\", id = 142)\n" +
                "   void asyncStoreShutdownTimeoutTooHigh(long configuredAsyncStopTimeout,\n" +
                "      long cacheStopTimeout, long asyncStopTimeout);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Received a key that doesn't map to this node: %s, mapped to %s\", id = 143)\n" +
                "   void keyDoesNotMapToLocalNode(Object key, Collection<Address> nodes);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Failed loading value for key %s from cache store\", id = 144)\n" +
                "   void failedLoadingValueFromCacheStore(Object key, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error invalidating keys from L1 after rehash\", id = 147)\n" +
                "   void failedToInvalidateKeys(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Invalid %s value of %s. It can not be higher than %s which is %s\", id = 148)\n" +
                "   void invalidTimeoutValue(Object configName1, Object value1, Object configName2, Object value2);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Fetch persistent state and purge on startup are both disabled, cache may contain stale entries on startup\", id = 149)\n" +
                "   void staleEntriesWithoutFetchPersistentStateOrPurgeOnStartup();\n" +
                "\n" +
                "   @LogMessage(level = FATAL)\n" +
                "   @Message(value = \"Rehash command received on non-distributed cache. All the nodes in the cluster should be using the same configuration.\", id = 150)\n" +
                "   void rehashCommandReceivedOnNonDistributedCache();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error flushing to file: %s\", id = 151)\n" +
                "   void errorFlushingToFileChannel(FileChannel f, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Passivation configured without an eviction policy being selected. \" +\n" +
                "      \"Only manually evicted entities will be passivated.\", id = 152)\n" +
                "   void passivationWithoutEviction();\n" +
                "\n" +
                "   // Warning ISPN000153 removed as per ISPN-2554\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unable to unlock keys %2$s for transaction %1$s after they were rebalanced off node %3$s\", id = 154)\n" +
                "   void unableToUnlockRebalancedKeys(GlobalTransaction gtx, List<Object> keys, Address self, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unblocking transactions failed\", id = 159)\n" +
                "   void errorUnblockingTransactions(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not complete injected transaction.\", id = 160)\n" +
                "   void couldNotCompleteInjectedTransaction(@Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Using a batchMode transaction manager\", id = 161)\n" +
                "   void usingBatchModeTransactionManager();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Could not instantiate transaction manager\", id = 162)\n" +
                "   void couldNotInstantiateTransactionManager(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"FileCacheStore ignored an unexpected file %2$s in path %1$s. The store path should be dedicated!\", id = 163)\n" +
                "   void cacheLoaderIgnoringUnexpectedFile(Object parentPath, String name);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Rolling back to cache view %d, but last committed view is %d\", id = 164)\n" +
                "   void cacheViewRollbackIdMismatch(int committedViewId, int committedView);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Strict peer-to-peer is enabled but the JGroups channel was started externally - this is very likely to result in RPC timeout errors on startup\", id = 171)\n" +
                "   void warnStrictPeerToPeerWithInjectedChannel();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Custom interceptor %s has used @Inject, @Start or @Stop. These methods will not be processed. Please extend org.infinispan.interceptors.base.BaseCustomInterceptor instead, and your custom interceptor will have access to a cache and cacheManager.  Override stop() and start() for lifecycle methods.\", id = 173)\n" +
                "   void customInterceptorExpectsInjection(String customInterceptorFQCN);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unexpected error reading configuration\", id = 174)\n" +
                "   void errorReadingConfiguration(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unexpected error closing resource\", id = 175)\n" +
                "   void failedToCloseResource(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"The 'wakeUpInterval' attribute of the 'eviction' configuration XML element is deprecated. Setting the 'wakeUpInterval' attribute of the 'expiration' configuration XML element to %d instead\", id = 176)\n" +
                "   void evictionWakeUpIntervalDeprecated(Long wakeUpInterval);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"%s has been deprecated as a synonym for %s. Use one of %s instead\", id = 177)\n" +
                "   void randomCacheModeSynonymsDeprecated(String candidate, String mode, List<String> synonyms);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"stateRetrieval's 'alwaysProvideInMemoryState' attribute is no longer in use, \" +\n" +
                "         \"instead please make sure all instances of this named cache in the cluster have 'fetchInMemoryState' attribute enabled\", id = 178)\n" +
                "   void alwaysProvideInMemoryStateDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"stateRetrieval's 'initialRetryWaitTime' attribute is no longer in use.\", id = 179)\n" +
                "   void initialRetryWaitTimeDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"stateRetrieval's 'logFlushTimeout' attribute is no longer in use.\", id = 180)\n" +
                "   void logFlushTimeoutDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"stateRetrieval's 'maxProgressingLogWrites' attribute is no longer in use.\", id = 181)\n" +
                "   void maxProgressingLogWritesDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"stateRetrieval's 'numRetries' attribute is no longer in use.\", id = 182)\n" +
                "   void numRetriesDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"stateRetrieval's 'retryWaitTimeIncreaseFactor' attribute is no longer in use.\", id = 183)\n" +
                "   void retryWaitTimeIncreaseFactorDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"The stateRetrieval configuration element has been deprecated, \" +\n" +
                "         \"we're assuming you meant stateTransfer. Please see XML schema for more information.\", id = 184)\n" +
                "   void stateRetrievalConfigurationDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"hash's 'rehashEnabled' attribute has been deprecated. Please use stateTransfer.fetchInMemoryState instead\", id = 185)\n" +
                "   void hashRehashEnabledDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"hash's 'rehashRpcTimeout' attribute has been deprecated. Please use stateTransfer.timeout instead\", id = 186)\n" +
                "   void hashRehashRpcTimeoutDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"hash's 'rehashWait' attribute has been deprecated. Please use stateTransfer.timeout instead\", id = 187)\n" +
                "   void hashRehashWaitDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error while processing a commit in a two-phase transaction\", id = 188)\n" +
                "   void errorProcessing2pcCommitCommand(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"While stopping a cache or cache manager, one of its components failed to stop\", id = 189)\n" +
                "   void componentFailedToStop(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Use of the 'loader' element to configure a store is deprecated, please use the 'store' element instead\", id = 190)\n" +
                "   void deprecatedLoaderAsStoreConfiguration();\n" +
                "\n" +
                "   @LogMessage(level = DEBUG)\n" +
                "   @Message(value = \"When indexing locally a cache with shared cache loader, preload must be enabled\", id = 191)\n" +
                "   void localIndexingWithSharedCacheLoaderRequiresPreload();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"hash's 'numVirtualNodes' attribute has been deprecated. Please use hash.numSegments instead\", id = 192)\n" +
                "   void hashNumVirtualNodesDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"hash's 'consistentHash' attribute has been deprecated. Please use hash.consistentHashFactory instead\", id = 193)\n" +
                "   void consistentHashDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Failed loading keys from cache store\", id = 194)\n" +
                "   void failedLoadingKeysFromCacheStore(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error during rebalance for cache %s on node %s\", id = 195)\n" +
                "   void rebalanceError(String cacheName, Address node, @Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Failed to recover cluster state after the current node became the coordinator\", id = 196)\n" +
                "   void failedToRecoverClusterState(@Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Error updating cluster member list\", id = 197)\n" +
                "   void errorUpdatingMembersList(@Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Unable to register MBeans for default cache\", id = 198)\n" +
                "   void unableToRegisterMBeans();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Unable to register MBeans for named cache %s\", id = 199)\n" +
                "   void unableToRegisterMBeans(String cacheName);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Unable to register MBeans for cache manager\", id = 200)\n" +
                "   void unableToRegisterCacheManagerMBeans();\n" +
                "\n" +
                "   @LogMessage(level = TRACE)\n" +
                "   @Message(value = \"This cache is configured to backup to its own site (%s).\", id = 201)\n" +
                "   void cacheBackupsDataToSameSite(String siteName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Problems backing up data for cache %s to site %s: %s\", id = 202)\n" +
                "   void warnXsiteBackupFailed(String cacheName, String key, Object value);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"The rollback request for tx %s cannot be processed by the cache %s as this cache is not transactional!\", id=203)\n" +
                "   void cannotRespondToRollback(GlobalTransaction globalTransaction, String cacheName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"The commit request for tx %s cannot be processed by the cache %s as this cache is not transactional!\", id=204)\n" +
                "   void cannotRespondToCommit(GlobalTransaction globalTransaction, String cacheName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Trying to bring back an non-existent site (%s)!\", id=205)\n" +
                "   void tryingToBringOnlineNonexistentSite(String siteName);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not execute cancelation command locally %s\", id=206)\n" +
                "   void couldNotExecuteCancellationLocally(String message);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not interrupt as no thread found for command uuid %s\", id=207)\n" +
                "   void couldNotInterruptThread(UUID id);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"No live owners found for segment %d of cache %s. Current owners are:  %s. Faulty owners: %s\", id=208)\n" +
                "   void noLiveOwnersFoundForSegment(int segmentId, String cacheName, Collection<Address> owners, Collection<Address> faultySources);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Failed to retrieve transactions for segments %s of cache %s from node %s\", id=209)\n" +
                "   void failedToRetrieveTransactionsForSegments(Collection<Integer> segments, String cacheName, Address source, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Failed to request segments %s of cache %s from node %s (node will not be retried)\", id=210)\n" +
                "   void failedToRequestSegments(Collection<Integer> segments, String cacheName, Address source, @Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unable to load %s from any of the following classloaders: %s\", id=213)\n" +
                "   void unableToLoadClass(String classname, String classloaders, @Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to remove entry under %s from cache store after activation\", id = 214)\n" +
                "   void unableToRemoveEntryAfterActivation(Object key, @Cause Exception e);\n" +
                "\n" +
                "   @Message(value = \"Unknown migrator %s\", id=215)\n" +
                "   Exception unknownMigrator(String migratorName);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"%d entries migrated to cache %s in %s\", id = 216)\n" +
                "   void entriesMigrated(long count, String name, String prettyTime);\n" +
                "\n" +
                "   @Message(value = \"Received exception from %s, see cause for remote stack trace\", id = 217)\n" +
                "   RemoteException remoteException(Address sender, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Timeout while waiting for the transaction validation. The command will not be processed. \" +\n" +
                "         \"Transaction is %s\", id = 218)\n" +
                "   void timeoutWaitingUntilTransactionPrepared(String globalTx);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Shutdown while handling command %s\", id = 219)\n" +
                "   void shutdownHandlingCommand(ReplicableCommand command);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Problems un-marshalling remote command from byte buffer\", id = 220)\n" +
                "   void errorUnMarshallingCommand(@Cause Throwable throwable);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unknown response value [%s]. Expected [%s]\", id = 221)\n" +
                "   void unexpectedResponse(String actual, String expected);\n" +
                "\n" +
                "   @Message(value = \"Custom interceptor missing class\", id = 222)\n" +
                "   CacheConfigurationException customInterceptorMissingClass();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Custom interceptor '%s' does not extend BaseCustomInterceptor, which is recommended\", id = 223)\n" +
                "   void suggestCustomInterceptorInheritance(String customInterceptorClassName);\n" +
                "\n" +
                "   @Message(value = \"Custom interceptor '%s' specifies more than one position\", id = 224)\n" +
                "   CacheConfigurationException multipleCustomInterceptorPositions(String customInterceptorClassName);\n" +
                "\n" +
                "   @Message(value = \"Custom interceptor '%s' doesn't specify a position\", id = 225)\n" +
                "   CacheConfigurationException missingCustomInterceptorPosition(String customInterceptorClassName);\n" +
                "\n" +
                "   @Message(value = \"Error while initializing SSL context\", id = 226)\n" +
                "   CacheConfigurationException sslInitializationException(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Support for concurrent updates can no longer be configured (it is always enabled by default)\", id = 227)\n" +
                "   void warnConcurrentUpdateSupportCannotBeConfigured();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Failed to recover cache %s state after the current node became the coordinator\", id = 228)\n" +
                "   void failedToRecoverCacheState(String cacheName, @Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"Unexpected initial version type (only NumericVersion instances supported): %s\", id = 229)\n" +
                "   IllegalArgumentException unexpectedInitialVersion(String className);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Failed to start rebalance for cache %s\", id = 230)\n" +
                "   void rebalanceStartError(String cacheName, @Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value=\"Cache mode should be DIST or REPL, rather than %s\", id = 231)\n" +
                "   IllegalStateException requireDistOrReplCache(String cacheType);\n" +
                "\n" +
                "   @Message(value=\"Cache is in an invalid state: %s\", id = 232)\n" +
                "   IllegalStateException invalidCacheState(String cacheState);\n" +
                "\n" +
                "   @Message(value=\"Waiting on work threads latch failed: %s\", id = 233)\n" +
                "   TimeoutException waitingForWorkerThreadsFailed(CountDownLatch latch);\n" +
                "\n" +
                "   @Message(value = \"Root element for %s already registered in ParserRegistry\", id = 234)\n" +
                "   IllegalArgumentException parserRootElementAlreadyRegistered(QName qName);\n" +
                "\n" +
                "   @Message(value = \"Configuration parser %s does not declare any Namespace annotations\", id = 235)\n" +
                "   CacheConfigurationException parserDoesNotDeclareNamespaces(String name);\n" +
                "\n" +
                "   @Message(value = \"Purging expired entries failed\", id = 236)\n" +
                "   PersistenceException purgingExpiredEntriesFailed(@Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"Waiting for expired entries to be purge timed out\", id = 237)\n" +
                "   PersistenceException timedOutWaitingForExpiredEntriesToBePurged(@Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"Directory %s does not exist and cannot be created!\", id = 238)\n" +
                "   CacheConfigurationException directoryCannotBeCreated(String path);\n" +
                "\n" +
                "   @Message(value=\"Cache manager is shutting down, so type write externalizer for type=%s cannot be resolved\", id = 239)\n" +
                "   IOException externalizerTableStopped(String className);\n" +
                "\n" +
                "   @Message(value=\"Cache manager is shutting down, so type (id=%d) cannot be resolved. Interruption being pushed up.\", id = 240)\n" +
                "   IOException pushReadInterruptionDueToCacheManagerShutdown(int readerIndex, @Cause InterruptedException cause);\n" +
                "\n" +
                "   @Message(value=\"Cache manager is %s and type (id=%d) cannot be resolved (thread not interrupted)\", id = 241)\n" +
                "   CacheException cannotResolveExternalizerReader(ComponentStatus status, int readerIndex);\n" +
                "\n" +
                "   @Message(value=\"Missing foreign externalizer with id=%s, either externalizer was not configured by client, or module lifecycle implementation adding externalizer was not loaded properly\", id = 242)\n" +
                "   CacheException missingForeignExternalizer(int foreignId);\n" +
                "\n" +
                "   @Message(value=\"Type of data read is unknown. Id=%d is not amongst known reader indexes.\", id = 243)\n" +
                "   CacheException unknownExternalizerReaderIndex(int readerIndex);\n" +
                "\n" +
                "   @Message(value=\"AdvancedExternalizer's getTypeClasses for externalizer %s must return a non-empty set\", id = 244)\n" +
                "   CacheConfigurationException advanceExternalizerTypeClassesUndefined(String className);\n" +
                "\n" +
                "   @Message(value=\"Duplicate id found! AdvancedExternalizer id=%d for %s is shared by another externalizer (%s). Reader index is %d\", id = 245)\n" +
                "   CacheConfigurationException duplicateExternalizerIdFound(int externalizerId, Class<?> typeClass, String otherExternalizer, int readerIndex);\n" +
                "\n" +
                "   @Message(value=\"Internal %s externalizer is using an id(%d) that exceeded the limit. It needs to be smaller than %d\", id = 246)\n" +
                "   CacheConfigurationException internalExternalizerIdLimitExceeded(AdvancedExternalizer<?> ext, int externalizerId, int maxId);\n" +
                "\n" +
                "   @Message(value=\"Foreign %s externalizer is using a negative id(%d). Only positive id values are allowed.\", id = 247)\n" +
                "   CacheConfigurationException foreignExternalizerUsingNegativeId(AdvancedExternalizer<?> ext, int externalizerId);\n" +
                "\n" +
                "   @Message(value =  \"Invalid cache loader configuration!!  Only ONE cache loader may have fetchPersistentState set \" +\n" +
                "         \"to true.  Cache will not start!\", id = 248)\n" +
                "   CacheConfigurationException multipleCacheStoresWithFetchPersistentState();\n" +
                "\n" +
                "   @Message(value =  \"The cache loader configuration %s does not specify the loader class using @ConfigurationFor\", id = 249)\n" +
                "   CacheConfigurationException loaderConfigurationDoesNotSpecifyLoaderClass(String className);\n" +
                "\n" +
                "   @Message(value = \"Invalid configuration, expecting '%s' got '%s' instead\", id = 250)\n" +
                "   CacheConfigurationException incompatibleLoaderConfiguration(String expected, String actual);\n" +
                "\n" +
                "   @Message(value = \"Cache Loader configuration cannot be null\", id = 251)\n" +
                "   CacheConfigurationException cacheLoaderConfigurationCannotBeNull();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error executing parallel store task\", id = 252)\n" +
                "   void errorExecutingParallelStoreTask(@Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"Invalid Cache Loader class: %s\", id = 253)\n" +
                "   CacheConfigurationException invalidCacheLoaderClass(String name);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"The transport element's 'strictPeerToPeer' attribute is no longer in use.\", id = 254)\n" +
                "   void strictPeerToPeerDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Error while processing prepare\", id = 255)\n" +
                "   void errorProcessingPrepare(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Configurator SAXParse error\", id = 256)\n" +
                "   void configuratorSAXParseError(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Configurator SAX error\", id = 257)\n" +
                "   void configuratorSAXError(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Configurator general error\", id = 258)\n" +
                "   void configuratorError(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Async store executor did not stop properly\", id = 259)\n" +
                "   void errorAsyncStoreNotStopped();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Exception executing command\", id = 260)\n" +
                "   void exceptionExecutingInboundCommand(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Failed to execute outbound transfer\", id = 261)\n" +
                "   void failedOutBoundTransferExecution(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Failed to enlist TransactionXaAdapter to transaction\", id = 262)\n" +
                "   void failedToEnlistTransactionXaAdapter(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"FIFO strategy is deprecated, LRU will be used instead\", id = 263)\n" +
                "   void warnFifoStrategyIsDeprecated();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Not using an L1 invalidation reaper thread. This could lead to memory leaks as the requestors map may grow indefinitely!\", id = 264)\n" +
                "   void warnL1NotHavingReaperThread();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to reset GlobalComponentRegistry after a failed restart!\", id = 265)\n" +
                "   void unableToResetGlobalComponentRegistryAfterRestart(@Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to reset GlobalComponentRegistry after a failed restart due to an exception of type %s with message %s. Use DEBUG level logging for full exception details.\", id = 266)\n" +
                "   void unableToResetGlobalComponentRegistryAfterRestart(String type, String message, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Problems creating interceptor %s\", id = 267)\n" +
                "   void unableToCreateInterceptor(Class type, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to broadcast evicts as a part of the prepare phase. Rolling back.\", id = 268)\n" +
                "   void unableToRollbackEvictionsDuringPrepare(@Cause Throwable e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Cache used for Grid metadata should be synchronous.\", id = 269)\n" +
                "   void warnGridFSMetadataCacheRequiresSync();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not commit local tx %s\", id = 270)\n" +
                "   void warnCouldNotCommitLocalTx(Object transactionDescription, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not rollback local tx %s\", id = 271)\n" +
                "   void warnCouldNotRollbackLocalTx(Object transactionDescription, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Exception removing recovery information\", id = 272)\n" +
                "   void warnExceptionRemovingRecovery(@Cause Exception e);\n" +
                "\n" +
                "   @Message(value = \"Indexing can not be enabled on caches in Invalidation mode\", id = 273)\n" +
                "   CacheConfigurationException invalidConfigurationIndexingWithInvalidation();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Persistence enabled without any CacheLoaderInterceptor in InterceptorChain!\", id = 274)\n" +
                "   void persistenceWithoutCacheLoaderInterceptor();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Persistence enabled without any CacheWriteInterceptor in InterceptorChain!\", id = 275)\n" +
                "   void persistenceWithoutCacheWriteInterceptor();\n" +
                "\n" +
                "   @Message(value = \"Could not find migration data in cache %s\", id = 276)\n" +
                "   CacheException missingMigrationData(String name);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Could not migrate key %s\", id = 277)\n" +
                "   void keyMigrationFailed(String key, @Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"Indexing can only be enabled if infinispan-query.jar is available on your classpath, and this jar has not been detected.\", id = 278)\n" +
                "   CacheConfigurationException invalidConfigurationIndexingWithoutModule();\n" +
                "\n" +
                "   @Message(value = \"Failed to read stored entries from file. Error in file %s at offset %d\", id = 279)\n" +
                "   PersistenceException errorReadingFileStore(String path, long offset);\n" +
                "\n" +
                "   @Message(value = \"Caught exception [%s] while invoking method [%s] on listener instance: %s\", id = 280)\n" +
                "   CacheListenerException exceptionInvokingListener(String name, Method m, Object target, @Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"%s reported that a third node was suspected, see cause for info on the node that was suspected\", id = 281)\n" +
                "   SuspectException thirdPartySuspected(Address sender, @Cause SuspectException e);\n" +
                "\n" +
                "   @Message(value = \"Cannot enable Invocation Batching when the Transaction Mode is NON_TRANSACTIONAL, set the transaction mode to TRANSACTIONAL\", id = 282)\n" +
                "   CacheConfigurationException invocationBatchingNeedsTransactionalCache();\n" +
                "\n" +
                "   @Message(value = \"A cache configured with invocation batching can't have recovery enabled\", id = 283)\n" +
                "   CacheConfigurationException invocationBatchingCannotBeRecoverable();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Problem encountered while installing cluster listener\", id = 284)\n" +
                "   void clusterListenerInstallationFailure(@Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Issue when retrieving cluster listeners from %s response was %s\", id = 285)\n" +
                "   void unsuccessfulResponseForClusterListeners(Address address, Response response);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Issue when retrieving cluster listeners from %s\", id = 286)\n" +
                "   void exceptionDuringClusterListenerRetrieval(Address address, @Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"Unauthorized access: subject '%s' lacks '%s' permission\", id = 287)\n" +
                "   SecurityException unauthorizedAccess(String subject, String permission);\n" +
                "\n" +
                "   @Message(value = \"A principal-to-role mapper has not been specified\", id = 288)\n" +
                "   CacheConfigurationException invalidPrincipalRoleMapper();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to send X-Site state chunk to '%s'.\", id = 289)\n" +
                "   void unableToSendXSiteState(String site, @Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to wait for X-Site state chunk ACKs from '%s'.\", id = 290)\n" +
                "   void unableToWaitForXSiteStateAcks(String site, @Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to apply X-Site state chunk.\", id = 291)\n" +
                "   void unableToApplyXSiteState(@Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unrecognized attribute %s.  Please check your configuration.  Ignoring!!\", id = 292)\n" +
                "   void unrecognizedAttribute(String property);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Ignoring XML attribute %s, please remove from configuration file\", id = 293)\n" +
                "   void ignoreXmlAttribute(Object attribute);\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Ignoring XML element %s, please remove from configuration file\", id = 294)\n" +
                "   void ignoreXmlElement(Object element);\n" +
                "\n" +
                "   @Message(value = \"No thread pool with name %s found\", id = 295)\n" +
                "   CacheConfigurationException undefinedThreadPoolName(String name);\n" +
                "\n" +
                "   @Message(value = \"Attempt to add a %s permission to a SecurityPermissionCollection\", id = 296)\n" +
                "   IllegalArgumentException invalidPermission(Permission permission);\n" +
                "\n" +
                "   @Message(value = \"Attempt to add a permission to a read-only SecurityPermissionCollection\", id = 297)\n" +
                "   SecurityException readOnlyPermissionCollection();\n" +
                "\n" +
                "   @LogMessage(level = DEBUG)\n" +
                "   @Message(value = \"Using internal security checker\", id = 298)\n" +
                "   void authorizationEnabledWithoutSecurityManager();\n" +
                "\n" +
                "   @Message(value = \"Unable to acquire lock after %s for key %s and requestor %s. Lock is held by %s, while request came from %s\", id = 299)\n" +
                "   TimeoutException unableToAcquireLock(String timeout, Object key, Object requestor, Object owner, String origin);\n" +
                "\n" +
                "   @Message(value = \"There was an exception while processing retrieval of entry values\", id = 300)\n" +
                "   CacheException exceptionProcessingEntryRetrievalValues(@Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"Iterator response for identifier %s encountered unexpected exception\", id = 301)\n" +
                "   CacheException exceptionProcessingIteratorResponse(UUID identifier, @Cause Throwable cause);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Issue when retrieving transactions from %s, response was %s\", id = 302)\n" +
                "   void unsuccessfulResponseRetrievingTransactionsForSegments(Address address, Response response);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"More than one configuration file with specified name on classpath. The first one will be used:\\n %s\", id = 304)\n" +
                "   void ambiguousConfigurationFiles(String files);\n" +
                "\n" +
                "   @Message(value = \"Cluster is operating in degraded mode because of node failures.\", id = 305)\n" +
                "   AvailabilityException partitionDegraded();\n" +
                "\n" +
                "   @Message(value = \"Key '%s' is not available. Not all owners are in this partition\", id = 306)\n" +
                "   AvailabilityException degradedModeKeyUnavailable(Object key);\n" +
                "\n" +
                "   @Message(value = \"Cannot clear when the cluster is partitioned\", id = 307)\n" +
                "   AvailabilityException clearDisallowedWhilePartitioned();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Rebalancing enabled\", id = 308)\n" +
                "   void rebalancingEnabled();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Rebalancing suspended\", id = 309)\n" +
                "   void rebalancingSuspended();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Starting cluster-wide rebalance for cache %s, topology %s\", id = 310)\n" +
                "   void startRebalance(String cacheName, CacheTopology cacheTopology);\n" +
                "\n" +
                "   @LogMessage(level = DEBUG)\n" +
                "   @Message(value = \"Received a command from an outdated topology, returning the exception to caller\", id = 311)\n" +
                "   void outdatedTopology(@Cause Throwable oe);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Cache %s lost data because of graceful leaver %s\", id = 312)\n" +
                "   void lostDataBecauseOfGracefulLeaver(String cacheName, Address leaver);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Cache %s lost data because of abrupt leavers %s\", id = 313)\n" +
                "   void lostDataBecauseOfAbruptLeavers(String cacheName, Collection<Address> leavers);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Cache %s lost at least half of the stable members, possible split brain causing data inconsistency. Current members are %s, lost members are %s, stable members are %s\", id = 314)\n" +
                "   void minorityPartition(String cacheName, Collection<Address> currentMembers, Collection<Address> lostMembers, Collection<Address> stableMembers);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Unexpected availability mode %s for cache %s partition %s\", id = 315)\n" +
                "   void unexpectedAvailabilityMode(AvailabilityMode availabilityMode, String cacheName, CacheTopology cacheTopology);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Cache %s lost data because of graceful leaver %s, entering degraded mode\", id = 316)\n" +
                "   void enteringDegradedModeGracefulLeaver(String cacheName, Address leaver);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Cache %s lost data because of abrupt leavers %s, assuming a network split and entering degraded mode\", id = 317)\n" +
                "   void enteringDegradedModeLostData(String cacheName, Collection<Address> leavers);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"Cache %s lost at least half of the stable members, assuming a network split and entering degraded mode. Current members are %s, lost members are %s, stable members are %s\", id = 318)\n" +
                "   void enteringDegradedModeMinorityPartition(String cacheName, Collection<Address> currentMembers, Collection<Address> lostMembers, Collection<Address> stableMembers);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"After merge (or coordinator change), cache %s still hasn't recovered all its data and must stay in degraded mode. Current members are %s, lost members are %s, stable members are %s\", id = 319)\n" +
                "   void keepingDegradedModeAfterMergeDataLost(String cacheName, Collection<Address> currentMembers, Collection<Address> lostMembers, Collection<Address> stableMembers);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"After merge (or coordinator change), cache %s still hasn't recovered a majority of members and must stay in degraded mode. Current members are %s, lost members are %s, stable members are %s\", id = 320)\n" +
                "   void keepingDegradedModeAfterMergeMinorityPartition(String cacheName, Collection<Address> currentMembers, Collection<Address> lostMembers, Collection<Address> stableMembers);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Cyclic dependency detected between caches, stop order ignored\", id = 321)\n" +
                "   void stopOrderIgnored();\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to re-start x-site state transfer to site %s\", id = 322)\n" +
                "   void failedToRestartXSiteStateTransfer(String siteName, @Cause Throwable cause);\n" +
                "\n" +
                "   @Message(value = \"%s is in 'TERMINATED' state and so it does not accept new invocations. \" +\n" +
                "         \"Either restart it or recreate the cache container.\", id = 323)\n" +
                "   IllegalLifecycleStateException cacheIsTerminated(String cacheName);\n" +
                "\n" +
                "   @Message(value = \"%s is in 'STOPPING' state and this is an invocation not belonging to an on-going transaction, so it does not accept new invocations. \" +\n" +
                "         \"Either restart it or recreate the cache container.\", id = 324)\n" +
                "   IllegalLifecycleStateException cacheIsStopping(String cacheName);\n" +
                "\n" +
                "   @Message (value=\"Creating tmp cache %s timed out waiting for rebalancing to complete on node %s \", id=325)\n" +
                "   RuntimeException creatingTmpCacheTimedOut(String cacheName, Address address);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Remote transaction %s timed out. Rolling back after %d ms\", id = 326)\n" +
                "   void remoteTransactionTimeout(GlobalTransaction gtx, long ageMilliSeconds);\n" +
                "\n" +
                "   @Message(value = \"Cannot find a parser for element '%s' in namespace '%s'. Check that your configuration is up-to date for this version of Infinispan.\", id = 327)\n" +
                "   CacheConfigurationException unsupportedConfiguration(String element, String namespaceUri);\n" +
                "\n" +
                "   @LogMessage(level = DEBUG)\n" +
                "   @Message(value = \"Finished local rebalance for cache %s on node %s, topology id = %d\", id = 328)\n" +
                "   void rebalanceCompleted(String cacheName, Address node, int topologyId);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to read rebalancing status from coordinator %s\", id = 329)\n" +
                "   void errorReadingRebalancingStatus(Address coordinator, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Distributed task failed at %s. The task is failing over to be executed at %s\", id = 330)\n" +
                "   void distributedTaskFailover(Address failedAtAddress, Address failoverTarget, @Cause Exception e);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Unable to invoke method %s on Object instance %s \", id = 331)\n" +
                "   void unableToInvokeListenerMethod(Method m, Object target, @Cause Throwable e);\n" +
                "\n" +
                "   @Message(value = \"Remote transaction %s rolled back because originator is no longer in the cluster\", id = 332)\n" +
                "   CacheException orphanTransactionRolledBack(GlobalTransaction gtx);\n" +
                "\n" +
                "   @Message(value = \"The site must be specified.\", id = 333)\n" +
                "   CacheConfigurationException backupSiteNullName();\n" +
                "\n" +
                "   @Message(value = \"Using a custom failure policy requires a failure policy class to be specified.\", id = 334)\n" +
                "   CacheConfigurationException customBackupFailurePolicyClassNotSpecified();\n" +
                "\n" +
                "   @Message(value = \"Two-phase commit can only be used with synchronous backup strategy.\", id = 335)\n" +
                "   CacheConfigurationException twoPhaseCommitAsyncBackup();\n" +
                "\n" +
                "   @LogMessage(level = INFO)\n" +
                "   @Message(value = \"Finished cluster-wide rebalance for cache %s, topology id = %d\", id = 336)\n" +
                "   void clusterWideRebalanceCompleted(String cacheName, int topologyId);\n" +
                "\n" +
                "   @Message(value = \"The 'site' must be specified!\", id = 337)\n" +
                "   CacheConfigurationException backupMissingSite();\n" +
                "\n" +
                "   @Message(value = \"It is required to specify a 'failurePolicyClass' when using a custom backup failure policy!\", id = 338)\n" +
                "   CacheConfigurationException missingBackupFailurePolicyClass();\n" +
                "\n" +
                "   @Message(value = \"Null name not allowed (use 'defaultRemoteCache()' in case you want to specify the default cache name).\", id = 339)\n" +
                "   CacheConfigurationException backupForNullCache();\n" +
                "\n" +
                "   @Message(value = \"Both 'remoteCache' and 'remoteSite' must be specified for a backup'!\", id = 340)\n" +
                "   CacheConfigurationException backupForMissingParameters();\n" +
                "\n" +
                "   @Message(value = \"Cannot configure async properties for an sync cache. Set the cache mode to async first.\", id = 341)\n" +
                "   IllegalStateException asyncPropertiesConfigOnSyncCache();\n" +
                "\n" +
                "   @Message(value = \"Cannot configure sync properties for an async cache. Set the cache mode to sync first.\", id = 342)\n" +
                "   IllegalStateException syncPropertiesConfigOnAsyncCache();\n" +
                "\n" +
                "   @Message(value = \"Must have a transport set in the global configuration in \" +\n" +
                "               \"order to define a clustered cache\", id = 343)\n" +
                "   CacheConfigurationException missingTransportConfiguration();\n" +
                "\n" +
                "   @Message(value = \"reaperWakeUpInterval must be >= 0, we got %d\", id = 344)\n" +
                "   CacheConfigurationException invalidReaperWakeUpInterval(long timeout);\n" +
                "\n" +
                "   @Message(value = \"completedTxTimeout must be >= 0, we got %d\", id = 345)\n" +
                "   CacheConfigurationException invalidCompletedTxTimeout(long timeout);\n" +
                "\n" +
                "   @Message(value = \"Total Order based protocol not available for transaction mode %s\", id = 346)\n" +
                "   CacheConfigurationException invalidTxModeForTotalOrder(TransactionMode transactionMode);\n" +
                "\n" +
                "   @Message(value = \"Cache mode %s is not supported by Total Order based protocol\", id = 347)\n" +
                "   CacheConfigurationException invalidCacheModeForTotalOrder(String friendlyCacheModeString);\n" +
                "\n" +
                "   @Message(value = \"Total Order based protocol not available with recovery\", id = 348)\n" +
                "   CacheConfigurationException unavailableTotalOrderWithTxRecovery();\n" +
                "\n" +
                "   @Message(value = \"Total Order based protocol not available with %s\", id = 349)\n" +
                "   CacheConfigurationException invalidLockingModeForTotalOrder(LockingMode lockingMode);\n" +
                "\n" +
                "   @Message(value = \"Enabling the L1 cache is only supported when using DISTRIBUTED as a cache mode.  Your cache mode is set to %s\", id = 350)\n" +
                "   CacheConfigurationException l1OnlyForDistributedCache(String cacheMode);\n" +
                "\n" +
                "   @Message(value = \"Using a L1 lifespan of 0 or a negative value is meaningless\", id = 351)\n" +
                "   CacheConfigurationException l1InvalidLifespan();\n" +
                "\n" +
                "   @Message(value = \"Use of the replication queue is invalid when using DISTRIBUTED mode.\", id = 352)\n" +
                "   CacheConfigurationException noReplicationQueueDistributedCache();\n" +
                "\n" +
                "   @Message(value = \"Use of the replication queue is only allowed with an ASYNCHRONOUS cluster mode.\", id = 353)\n" +
                "   CacheConfigurationException replicationQueueOnlyForAsyncCaches();\n" +
                "\n" +
                "   @Message(value = \"Cannot define both interceptor class (%s) and interceptor instance (%s)\", id = 354)\n" +
                "   CacheConfigurationException interceptorClassAndInstanceDefined(String customInterceptorClassName, String customInterceptor);\n" +
                "\n" +
                "   @Message(value = \"Unable to instantiate loader/writer instance for StoreConfiguration %s\", id = 355)\n" +
                "   CacheConfigurationException unableToInstantiateClass(Class<?> storeConfigurationClass);\n" +
                "\n" +
                "   @Message(value = \"Maximum data container size is currently 2^48 - 1, the number provided was %s\", id = 356)\n" +
                "   CacheConfigurationException evictionSizeTooLarge(long value);\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"end() failed for %s\", id = 357)\n" +
                "   void xaResourceEndFailed(XAResource resource, @Cause Throwable t);\n" +
                "\n" +
                "   @Message(value = \"A cache configuration named %s already exists. This cannot be configured externally by the user.\", id = 358)\n" +
                "   CacheConfigurationException existingConfigForInternalCache(String name);\n" +
                "\n" +
                "   @Message(value = \"Keys '%s' are not available. Not all owners are in this partition\", id = 359)\n" +
                "   AvailabilityException degradedModeKeysUnavailable(Collection<?> keys);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"The xml element eviction-executor has been deprecated and replaced by expiration-executor, please update your configuration file.\", id = 360)\n" +
                "   void evictionExecutorDeprecated();\n" +
                "\n" +
                "   @Message(value = \"Cannot commit remote transaction %s as it was already rolled back\", id = 361)\n" +
                "   CacheException remoteTransactionAlreadyRolledBack(GlobalTransaction gtx);\n" +
                "\n" +
                "   @Message(value = \"Could not find status for remote transaction %s, please increase transaction.completedTxTimeout\", id = 362)\n" +
                "   TimeoutException remoteTransactionStatusMissing(GlobalTransaction gtx);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"No indexing service provider found for indexed filter of type %s\", id = 363)\n" +
                "   void noFilterIndexingServiceProviderFound(String filterClassName);\n" +
                "\n" +
                "   @Message(value = \"Attempted to register cluster listener of class %s, but listener is annotated as only observing pre events!\", id = 364)\n" +
                "   CacheException clusterListenerRegisteredWithOnlyPreEvents(Class<?> listenerClass);\n" +
                "\n" +
                "   @Message(value = \"Could not find the specified JGroups configuration file '%s'\", id = 365)\n" +
                "   CacheConfigurationException jgroupsConfigurationNotFound(String cfg);\n" +
                "\n" +
                "   @Message(value = \"Unable to add a 'null' Custom Cache Store\", id = 366)\n" +
                "   IllegalArgumentException unableToAddNullCustomStore();\n" +
                "\n" +
                "   @LogMessage(level = ERROR)\n" +
                "   @Message(value = \"There was an issue with topology update for topology: %s\", id = 367)\n" +
                "   void topologyUpdateError(int topologyId, @Cause Throwable t);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Memory approximation calculation for eviction is unsupported for the '%s' Java VM\", id = 368)\n" +
                "   void memoryApproximationUnsupportedVM(String javaVM);\n" +
                "\n" +
                "   @LogMessage(level = WARN)\n" +
                "   @Message(value = \"Ignoring asyncMarshalling configuration\", id = 369)\n" +
                "   void ignoreAsyncMarshalling();\n" +
                "\n" +
                "   @Message(value = \"Cache name '%s' cannot be used as it is a reserved, internal name\", id = 370)\n" +
                "   IllegalArgumentException illegalCacheName(String name);\n" +
                "\n" +
                "   @Message(value = \"Cannot remove cache configuration '%s' because it is in use\", id = 371)\n" +
                "   IllegalStateException configurationInUse(String configurationName);\n" +
                "}"

        CodeBertScore.computeCodeBertScore(text1, text2)
    }
}