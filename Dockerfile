# Use a valid Java 17 image
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy all files into container
COPY . .

# Compile Java source file
RUN javac Server.java

# Expose backend port
EXPOSE 8080

# Start the Java server
CMD ["java", "Server"]
