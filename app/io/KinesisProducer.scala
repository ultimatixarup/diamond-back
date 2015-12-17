package io

import com.gilt.gfc.kinesis.KinesisFactory
import com.gilt.gfc.kinesis.publisher.{KinesisPublisherConfig, PartitionKey, RawRecord}
import events.ZipEpocUploaded
import play.api.libs.json.Json

object ZipEpocUploadedProducer extends KinesisProducer {

  private lazy val stream = appConf.getString("aws.kinesis.events.zipEpocUploaded")
  val producer = KinesisFactory.newPublisher(stream, config, convertEvent)

  def convertEvent(evt: ZipEpocUploaded) = RawRecord(Json.stringify(Json.toJson(evt)).getBytes, PartitionKey(evt.getClass.getSimpleName))

}


trait KinesisProducer {
  lazy val appConf = play.Play.application().configuration()
  lazy val awsRegion = appConf.getString("aws.region")

  val config = new KinesisPublisherConfig {
    override def regionName = awsRegion
  }

}

