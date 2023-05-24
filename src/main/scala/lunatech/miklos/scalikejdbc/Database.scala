package lunatech.miklos.scalikejdbc

import org.h2.jdbcx.JdbcDataSource
import org.h2.tools.Server
import scalikejdbc.{ConnectionPool, DB, DataSourceConnectionPool, scalikejdbcSQLInterpolationImplicitDef}


object Database {

  def init(): ConnectionPool = {
    val pool = initDb()
    initTables(pool)

    pool
  }

  private def initDb(): ConnectionPool = {
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
}
