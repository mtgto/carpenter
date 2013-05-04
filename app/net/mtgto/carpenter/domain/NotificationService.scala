package net.mtgto.carpenter.domain

import org.pircbotx.PircBotX

// // currently, support IRC notification only
trait NotificationService {
  def notify(notification: Notification, message: String)
}

object NotificationService extends NotificationService {
  override def notify(notification: Notification, message: String) = {
    val bot = new PircBotX
    bot.setName(notification.userName)
    bot.setEncoding(notification.encoding)
    bot.connect(notification.hostname, notification.port)
    bot.joinChannel(notification.channelName)
    bot.sendNotice(notification.channelName, message)
    bot.disconnect()
  }
}
