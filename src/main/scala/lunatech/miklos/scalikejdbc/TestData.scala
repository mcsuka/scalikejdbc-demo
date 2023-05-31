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

  val orders: Seq[(String, Seq[OrderItem])] = Seq(
    ("o001", Seq(OrderItem("apple", 4), OrderItem("pear", 4))),
    ("o002", Seq(OrderItem("orange", 4), OrderItem("pineapple", 4))),
    ("o003", Seq(OrderItem("orange", 4), OrderItem("apple", 4))),
    ("o004", Seq(OrderItem("banana", 4), OrderItem("pear", 4))),
    ("o005", Seq(OrderItem("orange", 4), OrderItem("pear", 4))),
    ("o006", Seq(OrderItem("banana", 4), OrderItem("pineapple", 4))),
    ("o007", Seq(OrderItem("banana", 4), OrderItem("apple", 4))),
    ("o008", Seq(OrderItem("pineapple", 4)))
  )
}
