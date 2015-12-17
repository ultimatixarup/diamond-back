import play.api.libs.json.Json

import scala.util.parsing.json.JSONFormat

/**
  * Created by neo on 11/11/15.
  */
package object events {

  lazy val appConf = play.Play.application().configuration()
  lazy val s3Prefix = appConf.getString("aws.s3Prefix")

  trait DiamondbackEvent {

    def destinationKinesisStream: String

    def sourceKinesisStream: String
  }

}
