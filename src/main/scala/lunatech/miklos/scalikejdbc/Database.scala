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

    val pool = DataSourceConnectionPool(ds)
//    ConnectionPool.singleton(pool)
    pool
  }

//  private val customerSql =
//    sql"""create table customer (
//      |  customer_id VARCHAR(32) NOT NULL,
//      |  name VARCHAR(255) NOT NULL,
//      |  CONSTRAINT customer_pk PRIMARY KEY (customer_id)
//      |)""".stripMargin
//  private val orderSql =
//    sql"""create table order (
//      |  customer_id VARCHAR(32) NOT NULL,
//      |  order_id VARCHAR(32) NOT NULL,
//      |  order_status VARCHAR(32) NOT NULL,
//      |  CONSTRAINT order_pk PRIMARY KEY (order_id),
//      |  CONSTRAINT fk_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
//      |)""".stripMargin
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

  private val stock = Seq(
    ("apple", 10),
    ("banana", 10),
    ("pear", 10),
    ("orange", 10)
  ).map((id, amount) => Seq(id, amount))

  private def initTables(connectionPool: ConnectionPool): Unit = {
    DB(connectionPool.borrow()).localTx { implicit dbSession =>
      orderItemsSql.execute.apply()
      productStockSql.execute.apply()

      sql"INSERT INTO product_stock VALUES (?, ?)".batch(stock: _*).apply()
    }
  }
}
