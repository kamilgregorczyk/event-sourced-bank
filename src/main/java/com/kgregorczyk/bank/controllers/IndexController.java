package com.kgregorczyk.bank.controllers;


import spark.Route;

/**
 * Returns 200 when system is healthy.
 */
public class IndexController {

  public static Route healthCheck() {
    return (request, response) -> {
      response.status(200);
      return "System is OK";
    };
  }
}
