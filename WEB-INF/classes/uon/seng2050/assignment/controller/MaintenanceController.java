package uon.seng2050.assignment.controller;

import io.seanbailey.adapter.Model;
import io.seanbailey.adapter.exception.SQLAdapterException;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import uon.seng2050.assignment.View;
import uon.seng2050.assignment.annotation.Action;
import uon.seng2050.assignment.exception.HttpException;
import uon.seng2050.assignment.exception.HttpStatusCode;
import uon.seng2050.assignment.model.MaintenanceEvent;
import uon.seng2050.assignment.model.User;
import uon.seng2050.assignment.model.User.Role;

@WebServlet(urlPatterns = {"/maintenance", "/maintenance/*"})
public class MaintenanceController extends AuthenticatedController {


  /**
   * Handles all requests to this controller, and delegates them to more specific handlers.
   *
   * @param request HTTP request object
   * @param response HTTP response object
   * @throws HttpException if an exception state is encountered that would typically return a HTTP
   * status code.
   */
  @Override
  protected void handleRequest(HttpServletRequest request, HttpServletResponse response)
      throws HttpException, ServletException, IOException {

    super.handleRequest(request, response);

    // Authenticate user
    if (authenticate(request, response)) {
      User user = (User) request.getAttribute("currentUser");
      if(user.getRole().equals(Role.USER.name())) {
        redirect("/articles",request,response);
      }
      route(this, request, response);
    }

  }


  /**
   * Renders all maintenance events.
   *
   * @param request HTTP request object.
   * @param response HTTP response object.
   * @param params URL parameters.
   */
  @Action(route = "/maintenance/?")
  private void renderIndex(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException, SQLException, SQLAdapterException {
    List<Model> events = Model
        .all(MaintenanceEvent.class)
        .execute();

    request.setAttribute("events", events);
    render(View.MAINTENANCE, request, response);
  }


  /**
   * Renders a page for creating a new maintenance event.
   *
   * @param request HTTP request object.
   * @param response HTTP response object.
   * @param params URL parameters.
   */
  @Action(route = "/maintenance/new")
  private void renderNew(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    render(View.NEW_MAINTENANCE, request, response);
  }


  /**
   * Creates a new maintenance event.
   *
   * @param request HTTP request object.
   * @param response HTTP response object.
   * @param params URL parameters.
   */
  @Action(methods = "POST", route = "/maintenance")
  private void createMaintenanceEvent(HttpServletRequest request, HttpServletResponse response)
      throws ParseException, SQLException, SQLAdapterException, IOException, ServletException {
    String name = request.getParameter("eventName");
    String start = request.getParameter("eventDate");
    String finish = request.getParameter("eventEnd");
    MaintenanceEvent newEvent = new MaintenanceEvent();

    if(!name.isEmpty() && !start.isEmpty() && !finish.isEmpty()) {
      DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
      Date eventStart = format.parse(start);
      Date eventFinish = format.parse(finish);

      newEvent.generateID();
      newEvent.setTitle(name);
      newEvent.setStartAt(eventStart);
      newEvent.setFinishAt(eventFinish);
    }

    if(newEvent.save()) {
      redirect("/maintenance",request,response);
    }
    else {
      request.setAttribute("errors", newEvent.getErrors());
      render(View.NEW_MAINTENANCE, request, response);
    }

  }


  /**
   * Renders a page for editing an existing maintenance event.
   *
   * @param request HTTP request object.
   * @param response HTTP response object.
   */
  @Action(route = "/maintenance/:id;/edit")
  private void renderEdit(HttpServletRequest request, HttpServletResponse response, String id)
      throws ServletException, IOException, SQLException, SQLAdapterException, HttpException {

    // Retrieve issue in question
    List<Model> events = Model.find(MaintenanceEvent.class, "id", id).execute();

    // Ensure result set is not empty
    if (events.isEmpty()) {
      throw new HttpException(HttpStatusCode.PAGE_NOT_FOUND,
          "Could not find an event with the id " + id);
    }

    request.setAttribute("event", events.get(0));
    render(View.EDIT_MAINTENANCE, request, response);
  }


  /**
   * Edits an existing maintenance event.
   *
   * @param request HTTP request object.
   * @param response HTTP response object.
   */
  @Action(methods = {"PATCH", "PUT", "POST"}, route = "/maintenance/:id;")
  private void updateMaintenanceEvent(HttpServletRequest request, HttpServletResponse response,
      String id)
      throws ServletException, IOException, SQLException, SQLAdapterException, HttpException, ParseException {

    String name = request.getParameter("eventName");
    String start = request.getParameter("eventDate");
    String finish = request.getParameter("eventEnd");

    DateFormat format = new SimpleDateFormat("yyyy-MM-dd");

    List<Model> events = Model.find(MaintenanceEvent.class, "id", id).execute();
    if (events.isEmpty()) {
      throw new HttpException(HttpStatusCode.PAGE_NOT_FOUND,
          "Could not find an event with the id " + id);
    }
    MaintenanceEvent event = (MaintenanceEvent) events.get(0);
    if (name != null) {
      event.setTitle(name);
    }
    if (start != null) {
      Date eventStart = format.parse(start);
      event.setStartAt(eventStart);
    }
    if (finish != null) {
      Date eventFinish = format.parse(finish);
      event.setFinishAt(eventFinish);
    }
    if(event.update()){
      redirect("/maintenance", request, response);
    }
    else {
      request.setAttribute("errors",event.getErrors());
      render(View.EDIT_MAINTENANCE, request, response);
    }
  }


}
