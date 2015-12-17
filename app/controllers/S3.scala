package controllers

import java.io.{File, FileInputStream, FileOutputStream, IOException}
import java.util.zip.{ZipEntry, ZipInputStream}
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

import com.gilt.gfc.kinesis.KinesisFactory
import com.gilt.gfc.kinesis.consumer.KinesisConsumerConfig
import com.gilt.gfc.kinesis.publisher.RawRecord
import com.google.gson.JsonParser
import controllers.Application._
import events.ZipEpocUploaded
import io.ZipEpocUploadedProducer
import play.api.Logger
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.mvc._
import sun.misc.BASE64Encoder

import scala.concurrent.duration._

object S3 extends Controller{

  private final val acccess_key_id = "AKIAJCNREYL7KSUNFYEA"
  private final val secret_access_key = "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"
  implicit val timeout = 10.seconds

  implicit val rds = (
    (__ \ 'key).read[String] and
      (__ \ 'uuid).read[String] and
      (__ \ 'bucket).read[String] and
      (__ \ 'name).read[String]
    ) tupled

  val consumeConfig = new KinesisConsumerConfig {
    override def appName: String = "etl-unzip"
    override def regionName: String = "us-west-1"
  }

  def convert(record: RawRecord): Option[String] = Some(new String(record.data))

  val receiver = KinesisFactory.newReceiver("zip-epoc-uploaded", consumeConfig, convert)

  def onEvent(event: String) = Logger.info(s"Received event: $event")

  receiver.registerConsumer(onEvent)

  receiver.start()

  def success = Action(parse.urlFormEncoded) { request =>
    val data = request.body
    val key = data.get("key").map(_ (0))
    val uuid = data.get("uuid").map(_ (0))
    val bucket = data.get("bucket").map(_ (0))
    val name = data.get("name").map(_ (0))
    val etag = data.get("etag").map(_ (0))

    val event = new ZipEpocUploaded(name = name, key = key, bucket = bucket, uuid = uuid, etag = etag)

    ZipEpocUploadedProducer.producer.publish(event)

    Ok(Json.obj("s3url" -> s"$key : $uuid, $bucket, $name"))
    //    }.recoverTotal {
    //      e => BadRequest("Expecting Json")
    //    }
  }

  def signature = Action { implicit request =>
    val jsonParser = new JsonParser()
    val contentJson = jsonParser.parse(request.body.asJson.getOrElse("").toString())
    val headers = contentJson.getAsJsonObject.get("headers")
    var signature: String = ""

    try {
      if (headers == null) {
        val base64Policy = base64EncodePolicy(request.body.asJson.get.toString())
        signature = sign(base64Policy)
        Ok(Json.obj("policy" -> base64Policy, "signature" -> signature))
      } else {
        signature = sign(headers.getAsString())
        Ok(Json.obj("signature" -> signature))
      }
    } catch {
      case _ => BadRequest
    }
  }

  private def sign(toSign: String): String = {
    val hmac = Mac.getInstance("HmacSHA1")
    hmac.init(new SecretKeySpec(secret_access_key.getBytes("UTF-8"), "HmacSHA1"))
    val signature = (new BASE64Encoder()).encode(hmac.doFinal(toSign.getBytes("UTF-8"))).replaceAll("\n", "")
    signature
  }

  private def base64EncodePolicy(input: String) = {
    //    val policyJsonStr = jsonElement.toString
    val encoded = (new BASE64Encoder()).encode(input.getBytes("UTF-8")).replaceAll("\n", "").replaceAll("\r", "")
    encoded
  }

  def unzip = Action { implicit request =>
    val input = "/Users/neo/Downloads/unzip-test/singleEpoch.zip"
    val outFolder = "/Users/neo/Downloads/unzip-test/out"

    unZipIt(input, outFolder)

    Ok("Got your request")

  }

  private def unZipIt(zipFilePath: String, outputFolder: String): Unit = {
    val buffer = new Array[Byte](1024)

    try {
      val out = new File(outputFolder)
      if (!out.exists()) out.mkdir()

      val zis: ZipInputStream = new ZipInputStream(new FileInputStream(zipFilePath))
      var ze: ZipEntry = zis.getNextEntry

      while (ze != null) {
        val fileName = ze.getName
        val newFile = new File(out + File.separator + fileName)

        Logger.info(s"file unzip : " + newFile.getAbsoluteFile)

        new File(newFile.getParent).mkdirs()

        val fos = new FileOutputStream(newFile)

        var len: Int = zis.read(buffer)

        while (len > 0) {
          fos.write(buffer, 0, len)
          len = zis.read(buffer)
        }

        fos.close()
        ze = zis.getNextEntry
      }

      zis.closeEntry()
      zis.close()
    } catch {
      case e: IOException => Logger.error("exception caught: " + e.getMessage)
    }
  }

}

