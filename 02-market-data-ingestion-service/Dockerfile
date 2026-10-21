# Imagem base com o Java Runtime Environment (JRE) do Eclipse Temurin 17
FROM eclipse-temurin:21-jre-alpine

# Define o diretório de trabalho dentro do container
WORKDIR /app

# Argumento para o caminho do arquivo JAR gerado pelo Maven/Gradle
ARG JAR_FILE=./build/libs/*.jar

# Copia o arquivo JAR para dentro do container com o nome app.jar
COPY ${JAR_FILE} app.jar

# Expõe a porta padrão do Spring Boot (altere se sua aplicação usar outra)
EXPOSE 8080

# Comando para iniciar a aplicação
ENTRYPOINT ["java", "-jar", "./app.jar"]
