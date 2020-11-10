import GreeterMain.SayHello
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
/*
wiki 解释 https://zh.wikipedia.org/wiki/%E6%BC%94%E5%91%98%E6%A8%A1%E5%9E%8B
  1.） 规避锁问题
  2.） 通过消息间接互相影响

  Actor：
    - 发送有限数量的消息给其它Actor         send message --> other Actor
    - 创建有限的的新Actor                  create new Actor
    - 指定接收到下一个消息时要用到的行为  --》 receive  do ？？？
 */
// 1） 定义 Actors 和messages
// 每个的actor 要定义它能接收的消息类型， case类或者case object是个很好的选择==》不可变，支持模式匹配
// 方便在Actor里面利用这点进行


object Greeter{
  final case class Greet(whom:String,replyTo:ActorRef[Greeted])
  final case class Greeted(whom:String,from:ActorRef[Greet])

  // Behavior 里面指定了数据类型为Greet 所以 message能够调用到whom ， 也可以调用到replyTo

  // 对比上面的定义 这里就是描述指定接收到下一个消息时要用到的行为
  // Behavior  do ？？？？ apply方法返回值为一个行为，行为里面包含泛型为Greet--》一个case类
  /*
  receive 方法的定义，   整体是一个onMessage 描述的是触发之后要做的事情，这是一个函数。
    函数里面的参数有两个
    方法的返回值是一个Receive  接收对象
      1. ActorContext 上下文对象， 凡是上下文就是包含了它能感知的相关信息
      2. T 就是receive[] 中描述的类型
   def receive[T](onMessage: (ActorContext[T], T) => Behavior[T]): Receive[T] =
    new ReceiveImpl(onMessage)

    当前的实现是返回行为的same，根据文档的解释
    从消息处理中返回这个行为，以便建议系统重用之前的行为。提供这个行为是为了避免在没有必要的情况下重新创建当前行为的分配开销。
   */
  def apply():Behavior[Greet] =Behaviors.receive{
    (context,message) =>
      context.log.info("Hello {}",message.whom) // 记录日志， 消息是谁发过来的
      message.replyTo ! Greeted(message.whom,context.self) // 响应回去，响应一个 Greeted 包含了发送者的名，和上下文本身
      Behaviors.same // 返回一个行为对象
  }
}

object GreeterBot{
  def apply(max:Int):Behavior[Greeter.Greeted] = {
    bot(0,max)
  }

  private def bot(greetingCounter:Int,max:Int):Behavior[Greeter.Greeted] =
    Behaviors.receive{
      (context,message) =>
        val n = greetingCounter + 1 // 计数 数据独立
        context.log.info("Greeting{} for {}",n,message.whom)
        if(n == max) {
          Behaviors.stopped // 到达指定的数后停止
        } else {
          message.from ! Greeter.Greet(message.whom,context.self) // 还没到最大值，发送消息
          bot(n,max)
        }
    }
}

object GreeterMain{
  final case class SayHello(name:String)

  def apply(): Behavior[SayHello] =
    Behaviors.setup{
      context =>
        val greeter = context.spawn(Greeter(),"greeter")
        Behaviors.receiveMessage{
          message =>
            val replyTo = context.spawn(GreeterBot(max = 3),message.name)
            greeter ! Greeter.Greet(message.name,replyTo)
            Behaviors.same
        }
    }
}

object QuickStart extends App {
//  创建一个ActorSystem， 它继承了ActorRef  里面有 ! 方法 可以 tell msg
  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "AkkaQuickStart")
//  !  ==     def !(msg: T): Unit = ref.tell(msg)  通过引用，告诉消息，也就是发出这个消息
  // 注意不是GreeterMain里面有 ! 方法，这个greeterMain 是通过ActorSystem构建的 ActorSystem[GreeterMain.SayHello]
  // 是ActorSystem里面有 ! 方法
  greeterMain ! SayHello("NiniBaby") // 只能接受ASCII字符
}
