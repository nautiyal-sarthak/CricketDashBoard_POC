package com.cri.producer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class Producer {
	
	static ArrayList<String> bating = new ArrayList<>();
	static ArrayList<String> bowling = new ArrayList<>();

	public static void main(String[] args) {
		String topic = "test";
		String brokers = "localhost:9092";
		Properties props = SetProperties(brokers);
		KafkaProducer<String, String> producer = new KafkaProducer<String, String>(props);

		Scanner sc = new Scanner(System.in);
		long noOfOvers = 0;

		System.out.println("Number of overs:");
		noOfOvers = sc.nextInt();

		System.out.println("List then names of the bowlers(comma separated):");
		String bolstr = sc.next();
		String[] bollst = bolstr.split(",");
		bowling.addAll(Arrays.asList(bollst));


		System.out.println("List then names of the batsmen:");
		bating.add(sc.next());
		bating.add(sc.next());

		int noOfBalls = (int) (noOfOvers * 6);
		int over = 0;
		int ball = 0;

		String bowler = bowling.get(0);
		while (over < noOfOvers) {
			long runtime = new Date().getTime();
			JsonObjectBuilder dataBuilder = Json.createObjectBuilder();

			// Integer runs = (int) sc.nextInt();
			Random random = new Random();
			Integer runs = random.nextInt(7);
			if (runs == 5) {
				runs = 1;
			}
			ball += 1;
			try {

				dataBuilder.add("over", over).add("ball", ball)
						.add("run", runs).add("batsman", bating.get(0))
						.add("bowler", bowler);
				JsonObject testJsonObject = dataBuilder.build();
				System.out.println(testJsonObject.toString());
				producer.send(new ProducerRecord<String, String>(topic,
						testJsonObject.toString()));
				
				
				
				Thread.sleep(5000);
				if (runs % 2 == 1) {
					swap();
				}

				if (ball % 6 == 0) {
					over += 1;
					ball = 0;
					swap();
					if (over != noOfOvers) {
						bowler = swapBowler(bowler);
						Thread.sleep(10000);
					}
				}

				Thread.sleep(500);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		sc.close();
		producer.close();
	}

	public static Properties SetProperties(String brokers) {

		Properties props = new Properties();
		props.put("bootstrap.servers", brokers);
		props.put("acks", "all");
		props.put("client.id", "ProducerExample");
		props.put("key.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer",
				"org.apache.kafka.common.serialization.StringSerializer");
		return props;

	}

	public static void swap() {
		String temp = bating.get(0);
		bating.set(0, bating.get(1));
		bating.set(1, temp);
	}

	public static String swapBowler(String CurrentBowler) {
		int index = bowling.indexOf(CurrentBowler);
		String Output = "";
		if (index < bowling.size() - 1) {
			Output = bowling.get(index + 1);
		} else {
			Output = bowling.get(0);
		}

		return Output;
	}
}
