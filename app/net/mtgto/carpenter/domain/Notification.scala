package net.mtgto.carpenter.domain

// the destination of notification (TODO rename)
// currently, support IRC notification only
case class Notification(hostname: String, port: Int, userName: String, channelName: String, encoding: String)
