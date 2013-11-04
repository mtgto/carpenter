package net.mtgto.carpenter.domain

import java.nio.charset.Charset
import org.pircbotx.{MultiBotManager, Configuration, PircBotX}
import org.pircbotx.hooks.{Event, Listener}
import org.pircbotx.hooks.events.{ModeEvent, ConnectEvent}

// // currently, support IRC notification only
trait NotificationService {
  def notify(notification: Notification, message: String)
}

object NotificationService extends NotificationService {
  override def notify(notification: Notification, message: String) = {
    val manager = new MultiBotManager[PircBotX]()
    val config = new Configuration.Builder()
      .setName(notification.userName)
      .setEncoding(Charset.forName(notification.encoding))
      .setAutoNickChange(true)
      .addAutoJoinChannel(notification.channelName)
      .setServer(notification.hostname, notification.port)
      .addListener(new Listener[PircBotX]() {
        def onEvent(e: Event[PircBotX]) {
          e match {
            case e: ModeEvent[PircBotX] =>
              e.getBot.sendIRC().notice(notification.channelName, message)
              manager.stopAndWait()
              println("あああああああああああああああああああああああ")
            case e => println(e.getClass.getSimpleName)
          }
        }
      })
      .buildConfiguration()
    manager.addBot(config)
    manager.start()
  }
}
