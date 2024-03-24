package com.selsupjava;

import com.google.gson.Gson;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
public class CrptApi {

    public static void main(String[] args) throws IOException, InterruptedException {
        SpringApplication.run(CrptApi.class, args);
        //Задаю ограничение количеств,а запросов
        createDocument crptApi = new createDocument(TimeUnit.SECONDS, 5);

        //предствавим что у нас есть данные документа и подпись
        createDocument.Document document = new createDocument.Document();
        String signature = "some signature";

        //вызываем метод
        String response = crptApi.SendDoc(document, signature);
        System.out.println(response);
    }


    private static class createDocument {

        private final int requestLimit;

        private final long timeInterval;

        private final AtomicInteger requestCount = new AtomicInteger(0);

        private long lastRequestTime = System.currentTimeMillis();

        private final Gson gson = new Gson();

        private String url = "https://ismp.crpt.ru/api/v3/lk/documents/create";


        public createDocument(TimeUnit timeUnit, int requestLimit) {
            this.requestLimit = requestLimit;
            this.timeInterval = timeUnit.toMillis(1);
        }

        public String SendDoc(Document document, String signature) throws IOException, InterruptedException {

            //отслеживание количества запросов
            synchronized (this) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastRequestTime >= timeInterval) {
                    requestCount.set(0);
                    lastRequestTime = currentTime;
                }

                //проверка допустимого количества запросов
                if (requestCount.get() >= requestLimit) {
                    try {
                        wait(timeInterval - currentTime + lastRequestTime);
                        requestCount.set(0);
                        lastRequestTime = System.currentTimeMillis();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }

                //сериализация даннх в JSON формат
                String jsonDoc = gson.toJson(document);

                //HTTP запрос
                HttpClient client = HttpClient.newHttpClient();

                HttpRequest request = HttpRequest.newBuilder()
                        .header("Signature", signature)
                        .uri(URI.create(url)).POST(HttpRequest.BodyPublishers.ofString(jsonDoc))
                        .build();


                //выполненяем запрос и возвращаем рузельтаты ответа
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                requestCount.incrementAndGet();
                return response.body().toString();
            }
        }

        @Data
        private static class Document {
            private String description;
            private String participantInn;
            private String doc_id;
            private String doc_status;
            private String doc_type;
            private String importRequest;
            private String owner_inn;
            private String participant_inn;
            private String producer_inn;
            private String production_date;
            private String production_type;

            private Product product;

            private String reg_date;
            private String reg_number;
        }

        @Data
        private static class Product {
            private String certificate_document;
            private String certificate_document_date;
            private String certificate_document_number;
            private String owner_inn;
            private String producer_inn;
            private String production_date;
            private String tnved_code;
            private String uit_code;
            private String uitu_code;
        }
    }

}


