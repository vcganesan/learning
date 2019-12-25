package org.inceptez.spark.core

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext

object corefunctions1 {
  
  def main(args:Array[String])
  {
   val conf = new SparkConf().setAppName("Local-sparkcore").setMaster("local[*]")
   val sc = new SparkContext(conf)
   
   //sc.setLogLevel("ERROR")
   val rdd = sc.textFile("file:/home/hduser/hive/data/txns")
   rdd.takeSample(true, 20, 100);
   //rdd.first()
   println("Number of partition of the base file" + rdd.getNumPartitions);
   println("Repartitioning to 10 partitions as its reading with 1 partition only when read from the file")
   rdd.repartition(10);
   val rddsplit=rdd.map(x=>x.split(","))
   val rddexerjump = rddsplit.filter(x => x(4).toUpperCase.contains("EXERCISE") || x(5).toUpperCase.startsWith("JUMP"))
   //println("Coalesce to 1 partition as the volume of data is less")
   println("Count the filtered rdd")
   val rddexerjumpcnt= rddexerjump.count()
   //val rddexerjumpsum= rddexerjump.to map(x=>x._3)
   
   println("No of lines with exercise or jumping: " + rddexerjumpcnt)
   
  println(s"No of lines are $rddexerjumpcnt with exercise or jumping")
  
  //Try to convert this as a function
  if (rddexerjumpcnt > 0 && rddexerjumpcnt < 40000)
  rddexerjump.coalesce(2); 
  else 
  rddexerjump.coalesce(4);
  
         
  println("Number of partition of the base file" + rddexerjump.getNumPartitions); 
   
  val rddcredit = rddsplit.filter(x => !x.contains("credit"))
  val cnt = rddcredit.count()
  println(s"No of lines that does not contain Credit: $cnt")
   
   
   val rdd2 = rddsplit.filter(x => x(7) == "California" && x(8) == "cash")
   val rdd3 = rdd2.map(x => x(3).toDouble)
     
     rdd3.cache()
     rdd3.unpersist()
     
     import org.apache.spark.storage._
     rdd3.persist(StorageLevel.MEMORY_AND_DISK)
     
     val sumofsales = rdd3.sum()
     println("Sum of Sales: " + sumofsales)
     
     val sumofsalesreduce= rdd3.reduce((x,y)=>x+y);
     println("Sum of Sales using reduce : " + sumofsalesreduce)
     
     val maxofsalesreduce= rdd3.reduce((x,y)=>if (x>y) x else y );
     println("Max of Sales using reduce: " + sumofsalesreduce)
     
     val maxofsales = rdd3.max()
     println("Max sales value : " + maxofsales)
     
     val totalsales = rdd3.count()     
     println("Total no fo sales: " + totalsales)
         
     val minofsales = rdd3.min()
     println("Min sales value : " + minofsales)
     
     val avgofsales = sumofsales/rdd3.count()
     println("Avg sales value : " + avgofsales)
   
     rdd3.unpersist();
     
   val rddtrimupper=rdd.map(x=>x.split(","))
   .map(x=>(x(0),x(1),x(2),x(3),charlen(x(4))))
   
   rddtrimupper.take(10).foreach(println)

     val rddunion = rddsplit.union(rddexerjump)
     .map(x=>(x(0),x(1),x(2),x(3),x(4),x(5),x(6)))
  rddunion.take(10).foreach(println)
   //val rddunion2cols = rddunion.map(x=>(x._3,x._4))
 
  
  println("City wise count : ")
  val rddkvpair=rddsplit.map(x=>((x(6)),(x(3).toDouble))) 
  rddkvpair.countByKey().take(10).foreach(println)
  
  println("Transction wise count : ")
  val rddcntbyval=rddsplit.map(x=>(x(8))) 
  rddcntbyval.countByValue.take(10).foreach(println)
  
  println("City wise sum of amount : ")
  rddkvpair.reduceByKey(_+_).take(10).foreach(println)
  
  println("City wise minimum amount of sales: ")
  rddkvpair.reduceByKey((a,b)=> (if (a > b) a else b)).take(10).foreach(println)
  
  println("City wise minimum amount of sales calling methods: ")
  rddkvpair.reduceByKey((a,b)=> (mintrans(a,b))).take(10).foreach(println)

   println("Brodcast real example")
  
   val kvpair: Map[String,Int] = Map("credit" -> 2, "cash" -> 1)
  
   val broadcastkvpair=sc.broadcast(kvpair)
  
  val broadcastrdd=rddsplit.map(x=>(x(0),x(3).toDouble+broadcastkvpair.value(x(8))))
  broadcastrdd.take(4).foreach(println);
   

  broadcastrdd.saveAsTextFile("hdfs://localhost:54310/user/hduser/broadcastrdd")
  
  /// Join Scenario
  
  println("Join Scenario")
  
  val custrdd=sc.textFile("file:/home/hduser/hive/data/custs");
  
  custrdd.cache;
  
  val transkvpair = rdd.map(x => x.split(",")).map(x => (x(2),(x(0),x(1),x(3),x(5))))
  transkvpair.cache;
  
  val custrddkvpair = custrdd.map(x => x.split(",")).map(x => (x(0),(x(2),x(3))))
    
  val custtransjoin = transkvpair.join(custrddkvpair)
    
  val finaljoinrdd=custtransjoin.map(x=>(x._1,x._2._1._3))  
  finaljoinrdd.take(10).foreach(println)
  println("Writing the output to hdfs with the ~ delimiter using productiterator and mkstring")
  custtransjoin.map(x=>(x._1,x._2._1._3).productIterator.mkString("~")).saveAsTextFile("hdfs://localhost:54310//user/hduser/joinedout/")
  }
  
  def mintrans(a:Double,b:Double):Double=
  {
    if (a < b) a else b    
  }
  
  
  def charlen(a:String):String=
  {
    return(a.trim().toUpperCase())
  }
  
}





