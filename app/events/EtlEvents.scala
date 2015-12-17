package events

import play.api.libs.json.Json

trait EtlEvent extends DiamondbackEvent

case class ZipEpocUploaded(name: Option[String], bucket: Option[String], key: Option[String], uuid: Option[String], etag: Option[String]) extends EtlEvent {
  val destinationKinesisStream: String = appConf.getString("aws.kinesis.events.zipEpocUploaded")
  val sourceKinesisStream = ""

  def url: String = s"$s3Prefix/$bucket/$key"


  implicit val fmt = Json.format[ZipEpocUploaded]
}

object ZipEpocUploaded {
  implicit val fmt = Json.format[ZipEpocUploaded]
  val whatthefuck = "a"
}