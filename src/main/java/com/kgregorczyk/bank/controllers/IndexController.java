package com.kgregorczyk.bank.controllers;


import com.kgregorczyk.bank.controllers.dto.APIResponse;
import spark.Route;

/**
 * Returns 200 when system is healthy.
 */
public class IndexController {

  public IndexController() {
  }

  public static Route healthCheck() {
    return (request, response) -> new APIResponse("System is OK");
  }
}
