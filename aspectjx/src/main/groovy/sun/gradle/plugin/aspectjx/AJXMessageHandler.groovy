package sun.gradle.plugin.aspectjx


import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.gradle.api.GradleException

class AJXMessageHandler extends MessageHandler {

    @Override
    boolean handleMessage(IMessage message) {
        boolean handleMsgResult = super.handleMessage(message)
        switch (message.getKind()) {
            case IMessage.ABORT:
            case IMessage.ERROR:
            case IMessage.FAIL:
                AJXLog.d("AJX_ERROR: ${getStackTraceString(message.thrown)}")
                throw new GradleException(message.message, message.thrown)
            case IMessage.WARNING:
                AJXLog.d("AJX_WARN: " + message.message)
                break
            case IMessage.INFO:
                AJXLog.d("AJX_INFO: " + message.message)
                break
            case IMessage.DEBUG:
                AJXLog.d("AJX_DEBUG: " + message.message)
                break
            case IMessage.WEAVEINFO:
                AJXLog.d("AJX_WEAVE_INFO: " + message.message)
                break
            case IMessage.USAGE:
                AJXLog.d("AJX_USAGE: " + message.message)
                break
            case IMessage.TASKTAG:
                AJXLog.d("AJX_TASK_TAG: " + message.message)
                break
            default:
                break
        }
        return handleMsgResult
    }

    static String getStackTraceString(Throwable tr) {
        if (tr == null) {
            return ""
        }

        // This is to reduce the amount of log spew that apps do in the non-error
        // condition of the network being unavailable.
        Throwable t = tr
        while (t != null) {
            if (t instanceof UnknownHostException) {
                return ""
            }
            t = t.getCause()
        }

        StringWriter sw = new StringWriter()
        PrintWriter pw = new PrintWriter(sw)
        tr.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }

    @Override
    boolean hasAnyMessage(IMessage.Kind kind, boolean orGreater) {
        return super.hasAnyMessage(kind, orGreater)
    }
}