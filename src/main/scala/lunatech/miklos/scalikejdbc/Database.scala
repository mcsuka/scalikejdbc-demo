package lunatech.miklos.scalikejdbc

import org.h2.jdbcx.JdbcDataSource
import org.h2.tools.Server
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, scalikejdbcSQLInterpolationImplicitDef}

import scala.util.Try

object Database {

  def init(): ConnectionPool = {
    val pool = initDb()
    ConnectionPool.singleton(pool)
    
    initTables(pool)

    pool
  }

  private def initDb(): DataSourceConnectionPool = {
    val ds = JdbcDataSource()
    ds.setUrl("jdbc:h2:mem:myDb;DB_CLOSE_DELAY=-1")
    ds.setUser("sa")
    ds.setPassword("sa")

    DataSourceConnectionPool(ds)
  }

  private val orderItemsSql =
    sql"""create table order_items (
      |  order_id VARCHAR(32) NOT NULL,
      |  product_id VARCHAR(32) NOT NULL,
      |  quantity INTEGER NOT NULL,
      |  CONSTRAINT order_item_pk PRIMARY KEY (order_id, product_id)
      |)""".stripMargin
  private val productStockSql =
    sql"""create table product_stock (
         |  product_id VARCHAR(32) NOT NULL,
         |  available INTEGER NOT NULL,
         |  CONSTRAINT product_stock_pk PRIMARY KEY (product_id)
         |)""".stripMargin

  private def initTables(connectionPool: ConnectionPool): Unit = {
    DB(connectionPool.borrow()).localTx { implicit dbSession =>
      orderItemsSql.execute.apply()
      productStockSql.execute.apply()

      val stock = TestData.stock.map((id, amount) => Seq(id, amount))
      sql"INSERT INTO product_stock VALUES (?, ?)".batch(stock: _*).apply()
    }
  }

  def getOrders: Iterable[Order] =
    Try {
      DB(ConnectionPool.get().borrow()).readOnly { implicit session =>
        sql"select order_id, product_id, quantity from order_items"
          .map(rs => rs.string(1) -> OrderItem(rs.string(2), rs.int(3)))
          .list
          .apply()
          .groupBy((id, item) => id)
          .map((id, items) => Order(id, items.map(_._2)))
      }
    }.getOrElse(Seq.empty)
  
}

