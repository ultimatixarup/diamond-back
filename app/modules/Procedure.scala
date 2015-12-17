package modules

import java.util.Date
import play.api.db._
import play.api.Play.current
import anorm._
import anorm.SqlParser._
import scala.language.postfixOps
import play.api.Logger

case class Procedure(id: Long, name: String, address: String, s3url: String)

// Pagination
case class Page[A](items: Seq[A], page: Int, offset: Long, total: Long) {
  lazy val prev = Option(page - 1).filter(_ >= 0)
  lazy val next = Option(page + 1).filter(_ => (offset + items.size) < total)
}

object Procedure {

  val procedure = {
    get[Long]("procedure.id") ~
      get[String]("procedure.name") ~
      get[String]("procedure.address") ~
      get[String]("procedure.s3url") map {
      case id ~ name ~ address ~ s3url => Procedure(id, name, address, s3url)
    }
  }

  // -- Queries
  /**
    * Retrieve a procedure from the id.
    */
  def findById(id: Long): Option[Procedure] = {
    DB.withConnection { implicit connection =>
      SQL("select * from procedure where id = {id}").on('id -> id).as(procedure.singleOpt)
    }
  }

  /**
    * Return a page of (Procedure).
    *
    * @param page Page to display
    * @param pageSize Number of procedures per page
    * @param orderBy Procedure property used for sorting
    * @param filter Filter applied on the name column
    */
  def list(page: Int = 0, pageSize: Int = 10, orderBy: Int = 1, filter: String = "%"): Page[Procedure] = {

    val offest = pageSize * page

    DB.withConnection { implicit connection =>

      val procedures = SQL(
        """
          select * from procedure
          where procedure.name like {filter}
          order by {orderBy} nulls last
          limit {pageSize} offset {offset}
        """).on(
        'pageSize -> pageSize,
        'offset -> offest,
        'filter -> filter,
        'orderBy -> orderBy).as(procedure *)

      val totalRows = SQL(
        """
          select count(*) from procedure
          where procedure.name like {filter}
        """).on(
        'filter -> filter).as(scalar[Long].single)

      Page(procedures, page, offest, totalRows)

    }

  }

  /**
    * Retrieve all procedure.
    *
    * @return
    */
  def findAll(): List[Procedure] = {
    DB.withConnection { implicit connection =>
      try {
        SQL("select * from procedure order by name").as(procedure *)
      } catch {
        case ex: Exception => Logger.info("ERROR", ex); Nil
      }
    }
  }

  /**
    * Update a procedure.
    *
    * @param id The procedure id
    * @param procedure The procedure values.
    */
  def update(id: Long, procedure: Procedure): Int = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          update procedure
          set name = {name}, address = {address}, s3url = {s3url}
          where id = {id}
        """).on(
        'id -> id,
        'name -> procedure.name,
        'address -> procedure.address,
        's3url -> procedure.s3url).executeUpdate()
    }
  }

  /**
    * Insert a new procedure.
    *
    * @param procedure The procedure values.
    */
  def insert(procedure: Procedure): Option[Long] = {
    DB.withConnection { implicit connection =>
      SQL(
        """
          insert into procedure values (
    		{id}, {name}, {address}, {s3url}
          )
        """).on(
        'id -> Option.empty[Long],
        'name -> procedure.name,
        'address -> procedure.address,
        's3url -> procedure.s3url).executeInsert()
    }
  }

  /**
    * Delete a procedure.
    *
    * @param id Id of the procedure to delete.
    */
  def delete(id: Long): Int = {
    DB.withConnection { implicit connection =>
      SQL("delete from procedure where id = {id}").on('id -> id).executeUpdate()
    }
  }

}
