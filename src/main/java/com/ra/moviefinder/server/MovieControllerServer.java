package com.ra.moviefinder.server;

import com.ra.moviefinder.service.MovieControllerServiceImpl;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class MovieControllerServer {
	//	the MovieController gRPC server is configured to start on port  50051
	public static final int MOVIE_CONTROLLER_SERVICE_PORT = 50051;

	public static void main(String[] args) throws IOException, InterruptedException {
		Server server = ServerBuilder.forPort(MOVIE_CONTROLLER_SERVICE_PORT)
		                             .addService(new MovieControllerServiceImpl()) // MovieControllerServiceImpl class provides the service implementation  for this microservice.
		                             .build();
		server.start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.shutdown();
			System.out.println("Successfully stopped the server");
		}));
		server.awaitTermination();
	}
}
