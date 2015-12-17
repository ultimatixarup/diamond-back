package controllers

import java.util.concurrent.TimeoutException

import modules.Procedure
import play.api._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.concurrent.Promise
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future
import scala.concurrent.duration._

object Application extends Controller {

  implicit val timeout = 10.seconds
  implicit val procedureJsonFormat = Json.format[Procedure]
  /**
    * Describe the procedure form for edit and create screens
    */
  val procedureForm = Form(
    mapping(
      "id" -> ignored(0: Long),
      "name" -> nonEmptyText,
      "address" -> nonEmptyText,
      "s3url" -> nonEmptyText)(Procedure.apply)(Procedure.unapply))
  val home = Redirect(routes.Application.list())

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  def dashboard = Action {
    Ok(views.html.dashboard("Diamondback Dashboard"))
  }

  def etl = Action {
    Ok(views.html.etl())
  }

  def list = Action.async { implicit request =>
    val futPage: Future[List[Procedure]] = TimeoutFuture(Procedure.findAll)
    futPage.map(procedures => Ok(Json.toJson(procedures))).recover {
      case t: TimeoutException =>
        Logger.error("Problem found in procedure list process")
        InternalServerError(t.getMessage)
    }
  }

  /**
    * Display the 'edit form' of a existing Procedure.
    *
    * @param id Id of the procedure to edit
    */
  def edit(id: Long) = Action.async {
    val futureEmp: Future[Option[Procedure]] = TimeoutFuture(Procedure.findById(id))
    futureEmp.map {
      case Some(procedure) => Ok("")
      case None => NotFound
    }.recover {
      case t: TimeoutException =>
        Logger.error("Problem found in procedure edit process")
        InternalServerError(t.getMessage)
    }
  }

  /**
    * Handle the 'edit form' submission
    *
    * @param id Id of the procedure to edit
    */
  def update(id: Long) = Action.async { implicit request =>
    procedureForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest("")),
      procedure => {
        val futureUpdateEmp: Future[Int] = TimeoutFuture(Procedure.update(id, procedure))
        futureUpdateEmp.map { empId =>
          home.flashing("success" -> s"Procedure ${procedure.name} has been updated")
        }.recover {
          case t: TimeoutException =>
            Logger.error("Problem found in procedure update process")
            InternalServerError(t.getMessage)
        }
      })
  }

  /**
    * Handle the 'new procedure form' submission.
    */
  def save = Action.async { implicit request =>
    procedureForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest("")),
      procedure => {
        val futureUpdateEmp: Future[Option[Long]] = TimeoutFuture(Procedure.insert(procedure))
        futureUpdateEmp.map {
          case Some(empId) =>
            val msg = s"Procedure ${procedure.name} has been created"
            Logger.info(msg)
            home.flashing("success" -> msg)
          case None =>
            val msg = s"Procedure ${procedure.name} has not created"
            Logger.info(msg)
            home.flashing("error" -> msg)
        }.recover {
          case t: TimeoutException =>
            Logger.error("Problem found in procedure update process")
            InternalServerError(t.getMessage)
        }
      })
  }

  /**
    * Handle procedure deletion.
    */
  def delete(id: Long) = Action.async {
    val futureInt = TimeoutFuture(Procedure.delete(id))
    futureInt.map(i => home.flashing("success" -> "Procedure has been deleted")).recover {
      case t: TimeoutException =>
        Logger.error("Problem deleting procedure")
        InternalServerError(t.getMessage)
    }
  }

  object TimeoutFuture {

    def apply[A](block: => A)(implicit timeout: FiniteDuration): Future[A] = {

      val promise = scala.concurrent.Promise[A]()

      // if the promise doesn't have a value yet then this completes the future with a failure
      Promise.timeout(Nil, timeout).map(_ => promise.tryFailure(new TimeoutException("This operation timed out")))

      // this tries to complete the future with the value from block
      Future(promise.success(block))

      promise.future
    }

  }

}