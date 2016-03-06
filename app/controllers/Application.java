package controllers;

import models.User;
import models.utils.AppException;
import play.Logger;
import play.api.libs.json.Json;
import play.data.Form;
import play.data.validation.Constraints;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import static play.data.Form.form;

/**
 * Login and Logout.
 * User: yesnault
 */
public class Application extends Controller {

    public static Result GO_HOME = redirect(
            routes.Application.index()
    );

    public static Result GO_DASHBOARD = redirect(
            routes.Dashboard.index()
    );

    /**
     * Add the content-type json to response
     *
     * @param Result httpResponse
     *
     * @return Result
     */
    public Result jsonResult(Result httpResponse) {
        response().setContentType("application/json; charset=utf-8");
        return httpResponse;
    }

    public static final String STATUS_SUCCESS="{\"status\" : \"success\",";
    public static final String LOGIN_SUCCESS=",\"message\" : \"login successful\"}";
    public static final String LOGOUT_SUCCESS="\"message\" : \"logout successful\"}";
    public static final String DATA="\"data\" :";
    /**
     * Display the login page or dashboard if connected
     *
     * @return login page or dashboard
     */
    public Result index() {
        // Check that the email matches a confirmed user before we redirect
        String email = ctx().session().get("email");
        if (email != null) {
            User user = User.findByEmail(email);
            if (user != null && user.validated) {
                //return GO_DASHBOARD;
                return jsonResult(ok(STATUS_SUCCESS + play.libs.Json.toJson(user) + LOGIN_SUCCESS ));
            } else {
                Logger.debug("Clearing invalid session credentials");
                session().clear();
            }
        }

        return ok(index.render(form(Register.class), form(Login.class)));
    }

    /**
     * Login class used by Login Form.
     */
    public static class Login {

        @Constraints.Required
        public String email;
        @Constraints.Required
        public String password;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public Result validate() {

            User user = null;
            try {
                user = User.authenticate(email, password);
            } catch (AppException e) {
                //return Messages.get("error.technical");
                return ok("{\"status\" : \"failure\", \"message\" : \"technical error. please check config\"}");
            }
            if (user == null) {
               // return Messages.get("invalid.user.or.password");
                return ok("{\"status\" : \"failure\", \"message\" : \"invalid user name or password\"}");
            } else if (!user.validated) {
               // return Messages.get("account.not.validated.check.mail");
                return ok("{\"status\" : \"failure\", \"message\" : \"account not validated check email\"}");
            }
            return null;
        }

    }

    public static class Register {

        @Constraints.Required
        public String email;

        @Constraints.Required
        public String userName;

        @Constraints.Required
        public String inputPassword;

        @Constraints.Required
        public String firstName;

        @Constraints.Required
        public String lastName;

        /**
         * Validate the authentication.
         *
         * @return null if validation ok, string with details otherwise
         */
        public String validate() {
            if (isBlank(email)) {
                return "Email is required";
            }

            if (isBlank(userName)) {
                return "Full name is required";
            }

            if (isBlank(inputPassword)) {
                return "Password is required";
            }

            return null;
        }

        private boolean isBlank(String input) {
            return input == null || input.isEmpty() || input.trim().isEmpty();
        }
    }

    /**
     * Handle login form submission.
     *
     * @return Dashboard if auth OK or login form if auth KO
     */
    public Result authenticate() {
        Form<Login> loginForm = form(Login.class).bindFromRequest();

        Form<Register> registerForm = form(Register.class);

        if (loginForm.hasErrors()) {
            return badRequest(index.render(registerForm, loginForm));
        } else {
            User user = null;
            try {
                user = User.authenticate(loginForm.get().email, loginForm.get().password);
            }
            catch (IllegalStateException e){
                return ok("{\"status\" : \"failure\", \"message\" : \"invalid user name or password\"}");
            }
            catch (AppException e ) {
                //return Messages.get("error.technical");
                return ok("{\"status\" : \"failure\", \"message\" : \"technical error. please check config\"}");
            }
            /*session("email", loginForm.get().email);
            User user = User.findByEmail(loginForm.get().email);
            //return GO_DASHBOARD;*/
            if(user != null){
                return jsonResult(ok(STATUS_SUCCESS + DATA+ play.libs.Json.toJson(user) + LOGIN_SUCCESS ));
            }

        }
        return ok("{\"status\" : \"failure\", \"message\" : \"Invalid email or passoword\"}");
    }

    /**
     * Logout and clean the session.
     *
     * @return Index page
     */
    public Result logout(String auth_key) {
        try {
            User.findByAuthKey(auth_key).deleteAuth_key();
            session().clear();
        }
        catch (IllegalStateException e){
            return ok("{\"status\" : \"failure\", \"message\" : \"authorization key\"}");
        }
        //flash("success", Messages.get("youve.been.logged.out"));
        return jsonResult(ok(STATUS_SUCCESS+LOGOUT_SUCCESS));
        //return GO_HOME;
    }

}