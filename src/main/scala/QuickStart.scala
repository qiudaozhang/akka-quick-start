import GreeterMain.SayHello
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
// 1） 定义 Actors 和messages
// 每个的actor 要定义它能接收的消息类型， case类或者case object是个很好的选择==》不可变，支持模式匹配
// 方便在Actor里面利用这点进行


object Greeter{
  final case class Greet(whom:String,replyTo:ActorRef[Greeted])
  final case class Greeted(whom:String,from:ActorRef[Greet])

  // Behavior 里面指定了数据类型为Greet 所以 message能够调用到whom ， 也可以调用到replyTo
  def apply():Behavior[Greet] =Behaviors.receive{
    (context,message) =>
      context.log.info("Hello {}",message.whom)
      message.replyTo ! Greeted(message.whom,context.self)
      Behaviors.same
  }
}

object GreeterBot{
  def apply(max:Int):Behavior[Greeter.Greeted] = {
    bot(0,max)
  }

  private def bot(greetingCounter:Int,max:Int):Behavior[Greeter.Greeted] =
    Behaviors.receive{
      (context,message) =>
        val n = greetingCounter + 1
        context.log.info("Greeting{} for {}",n,message.whom)
        if(n == max) {
          Behaviors.stopped
        } else {
          message.from ! Greeter.Greet(message.whom,context.self)
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
//  创建一个Actor系统
  val greeterMain: ActorSystem[GreeterMain.SayHello] = ActorSystem(GreeterMain(), "AkkaQuickStart")
//  !  ==     def !(msg: T): Unit = ref.tell(msg)  通过引用，告诉消息，也就是发出这个消息
  greeterMain ! SayHello("Charles")
}
