# OneNet DSP API

The OneNet DSP services are used to provide data loading and consumption functionality within the connector. It makes server2server calls to the connector and centralized services.

## Build and create a docker image
To build the project and create a docker image run this command:
```
docker build -t <image_name> . --no-cache
```
example:
```
docker build -t twineu/onenet-dsp-api:1.0 . --no-cache
```

## Run locally with maven and Spring Boot
To run locally with maven and spring boot make sure you have maven and jdk 17 installed.

To avoid installing maven you can use the provided mvnw, example:
```
./mvnw clean install
./mvnw spring-boot:run
```
