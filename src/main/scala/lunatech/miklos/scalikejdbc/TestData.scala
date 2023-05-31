package lunatech.miklos.scalikejdbc

object TestData {
  
  private lazy val startTime: Long = System.currentTimeMillis()
  
  private def timestamp(): String = {
    String.format("%4d", System.currentTimeMillis() - startTime)
  }

  def log(msg: String): Unit = {
    println(s"${timestamp()} [${Thread.currentThread().getName}] $msg")
  }

  val stock: Seq[(String, Int)] = Seq(
    ("apple", 10),
    ("banana", 10),
    ("pear", 10),
    ("orange", 10),
    ("pineapple", 10)
  )

  val orders: Seq[Order] = Seq(
    Order("o001", Seq(OrderItem("apple", 4), OrderItem("pear", 4))),
    Order("o002", Seq(OrderItem("orange", 4), OrderItem("pineapple", 4))),
    Order("o003", Seq(OrderItem("orange", 4), OrderItem("apple", 4))),
    Order("o004", Seq(OrderItem("banana", 4), OrderItem("pear", 4))),
    Order("o005", Seq(OrderItem("orange", 4), OrderItem("pear", 4))),
    Order("o006", Seq(OrderItem("banana", 4), OrderItem("pineapple", 4))),
    Order("o007", Seq(OrderItem("banana", 4), OrderItem("apple", 4))),
    Order("o008", Seq(OrderItem("pineapple", 4)))
  )
}
