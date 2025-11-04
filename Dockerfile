# Use Java 17 (official)
FROM eclipse-temurin:17-jdk

# Working directory inside container
WORKDIR /app

# Copy all backend files (Server.java, pets.json, etc.)
COPY . .

# Compile Java code
RUN javac Server.java

# Expose default port (Render will override using $PORT)
EXPOSE 8000

# Run the server
CMD ["java", "Server"]
