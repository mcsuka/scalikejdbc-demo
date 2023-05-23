package lunatech.miklos.scalikejdbc

case class OrderItem(productId: String, quantity: Int)

case class Order(orderId: String, items: Seq[OrderItem])
