package com.cri.consumer

import java.io.StringReader
import java.util.concurrent.atomic.AtomicInteger

import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Assign
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import scala.tools.nsc.io.Path
import javax.json.Json
import java.util.concurrent.atomic.AtomicLong
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.kafka010.Subscribe
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request;
import org.apache.solr.client.solrj.impl.HttpSolrClient;

object Consumer {
  def main(args: Array[String]): Unit = {

    var numberOfball: AtomicInteger = new AtomicInteger(0)
    var numberofrunsScored: AtomicInteger = new AtomicInteger(0)
    var battingDetais = scala.collection.mutable.Map[String, (Int, Int, Int, Int)]()
    var bowlingDetais = scala.collection.mutable.Map[String, (Int, Int)]()
    var runPerover = scala.collection.mutable.Map[Int, Int]()

    val conf = new SparkConf().setAppName("simple").setMaster("local[4]")
    val sc = new SparkContext(conf)
    val ssc = new StreamingContext(sc, org.apache.spark.streaming.Durations.seconds(1))

    Logger.getLogger("org").setLevel(Level.FATAL)
    Logger.getLogger("akka").setLevel(Level.OFF)

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "localhost:9092",
      "key.deserializer" -> classOf[StringDeserializer],
      "value.deserializer" -> classOf[StringDeserializer],
      "group.id" -> "Sprk2",
      "auto.offset.reset" -> "latest",
      "enable.auto.commit" -> (false: java.lang.Boolean))

    //Note : the below code is to read all the mesages from all the partitions
    //    val topics = Array("cri")
    //    val stream = KafkaUtils.createDirectStream[String, String](
    //      ssc,
    //      PreferConsistent,
    //      Subscribe[String, String](topics, kafkaParams))

    ////Note : the below code is to read all the mesages from a perticular partition
    val partition0 = new TopicPartition("test", 0)
    val parlist = List(partition0)
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Assign[String, String](parlist, kafkaParams))

    var data = stream.map(jsons => jsons.value())
    data.foreachRDD(x => println(x.count()))
    //println(data.count())
    data.foreachRDD(rdd => {
      if (rdd.count() > 0) {
        var rows = rdd.collect()
        for (row <- rows) {
          var jsonReader = Json.createReader(new StringReader(row));
          var obj = jsonReader.readObject();
          jsonReader.close();

          println(obj.toString())
          battingDetais += (obj.getString("batsman") ->
            (
              {
                if (battingDetais.contains(obj.getString("batsman"))) {
                  battingDetais(obj.getString("batsman"))._1 + 1
                } else {
                  1
                }
              },
              {
                if (battingDetais.contains(obj.getString("batsman"))) {
                  battingDetais(obj.getString("batsman"))._2 + obj.getInt("run")
                } else {
                  obj.getInt("run")
                }
              }, {
                if (battingDetais.contains(obj.getString("batsman"))) {
                  if (obj.getInt("run").equals(4)) {
                    battingDetais(obj.getString("batsman"))._3 + 1
                  } else {
                    battingDetais(obj.getString("batsman"))._3
                  }
                } else {
                  if (obj.getInt("run").equals(4)) {
                    1
                  } else {
                    0
                  }
                }
              }, {
                if (battingDetais.contains(obj.getString("batsman"))) {
                  if (obj.getInt("run").equals(6)) {
                    battingDetais(obj.getString("batsman"))._4 + 1
                  } else {
                    battingDetais(obj.getString("batsman"))._4
                  }
                } else {
                  if (obj.getInt("run").equals(6)) {
                    1
                  } else {
                    0
                  }
                }
              }))

          bowlingDetais += (obj.getString("bowler") ->
            (
              {
                if (bowlingDetais.contains(obj.getString("bowler"))) {
                  bowlingDetais(obj.getString("bowler"))._1 + 1
                } else {
                  1
                }
              },
              {
                if (bowlingDetais.contains(obj.getString("bowler"))) {
                  bowlingDetais(obj.getString("bowler"))._2 + obj.getInt("run")
                } else {
                  obj.getInt("run")
                }
              }))

          runPerover += (obj.getInt("over") -> ({
            if (runPerover.contains(obj.getInt("over"))) {
              runPerover(obj.getInt("over")) + obj.getInt("run")
            } else {
              obj.getInt("run")
            }
          }))

          numberOfball.getAndAdd(1)
          numberofrunsScored.getAndAdd(obj.getInt("run"))

          println("Total Score : " + numberofrunsScored.get())
          println("Total balls : " + obj.getInt("over") + "." + obj.getInt("ball"))

          if (obj.getInt("ball").equals(6)) {
            println("--------------------------SUMMARY-------------------")

            println("team run rate : " + (numberofrunsScored.get().toFloat / (numberOfball.get().toFloat / 6)))
            println(" ")
            for (battman <- battingDetais) {
              println(battman._1 + " ball faced : " + battman._2._1)
              println(battman._1 + " runs scored : " + battman._2._2)
              println(battman._1 + " strike rate : " + ((battman._2._2.toFloat / battman._2._1.toFloat) * 100))
              println(battman._1 + " Number of 4s : " + battman._2._3)
              println(battman._1 + " Number of 6s : " + battman._2._4)
              println(" ")
            }
            println("****")
            for (bowling <- bowlingDetais) {
              println(bowling._1 + " overs bowled : " + (bowling._2._1 / 6))
              println(bowling._1 + " runs given : " + bowling._2._2)
              println(bowling._1 + " Bowling Avg : " + (bowling._2._2.toFloat / (bowling._2._1.toFloat / 6)))
              println(" ")
            }
            println("****")
            for (runs <- runPerover) {
              println("over :" + runs._1 + " Runs Scored :" + runs._2)
            }
          }
          println("---------------------------------------------------------")

          var client = getSolrClient()
          var doc1 = new SolrInputDocument()
          doc1.setField("overNumber", obj.getInt("over") + 1);
          if (obj.getInt("ball").equals(6)) {
            doc1.setField("ballNumber", (obj.getInt("over") + 1) + ".0");
          } else {
            doc1.setField("ballNumber", obj.getInt("over") + "." + obj.getInt("ball"));
          }

          doc1.setField("totalballs", numberOfball.get());
          doc1.setField("totalRuns", numberofrunsScored.get());
          doc1.setField("batteamrunrate", (numberofrunsScored.get().toFloat / (numberOfball.get().toFloat / 6)));
          doc1.setField("runscoredonthisball", obj.getInt("run"));
          doc1.setField("batname", obj.getString("batsman"));

          var bat = battingDetais.get(obj.getString("batsman"))
          var bowl = bowlingDetais.get(obj.getString("bowler"))

          doc1.setField("batbolplayed", bat.get._1);
          doc1.setField("batstrikerate", ((bat.get._2.toFloat / bat.get._1.toFloat) * 100));
          doc1.setField("bowlname", obj.getString("bowler"));
          doc1.setField("bowlerBallsNo", bowl.get._1);
          doc1.setField("bowlAvg", (bowl.get._2.toFloat / (bowl.get._1.toFloat / 6)));
          if (obj.getInt("ball").equals(6)) {
            var runs = runPerover.get(obj.getInt("over"))
            doc1.setField("runsscoredthisover", runs.get.toString());
          }

          var updateResponse = client.add("cri", doc1);
          client.commit("cri");

        }

      }
    })

    ssc.start() // Start the spark streaming
    ssc.awaitTermination();
  }

  private def getSolrClient(): SolrClient = {
    var solrUrl = "http://localhost:8983/solr";
    return new HttpSolrClient.Builder(solrUrl)
      .withConnectionTimeout(10000).withSocketTimeout(60000)
      .build();
  }
}