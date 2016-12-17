package ru.mipt.java2016.homework.g594.petrov.task4;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import ru.mipt.java2016.homework.base.task1.Calculator;
import ru.mipt.java2016.homework.base.task1.ParsingException;
import ru.mipt.java2016.homework.g594.petrov.task1.MegaCalculator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by philipp on 15.12.16.
 */


@RestController
public class CalculatorController {
    private static final Logger LOG = LoggerFactory.getLogger(CalculatorController.class);
    private static final HashSet<String> FUNCTIONS = new HashSet<>(Arrays.asList("max", "min", "sin", "cos", "tg", "sqrt", "abs", "sign", "log", "log2", "rnd", "pow"));
    @Autowired
    private Calculator calculator;

    @Autowired
    private BillingDao billingDao;

    @RequestMapping(path = "/ping", method = RequestMethod.GET, produces = "text/plain")
    public String echo() {
        return "OK\n";
    }

    @RequestMapping(path = "/", method = RequestMethod.GET, produces = "text/html")
    public String main(@RequestParam(required = false) String name) {
        if (name == null) {
            name = "world";
        }
        return "<html>" +
                "<head><title>PhilyaApp</title></head>" +
                "<body><h1>Hello, " + name + "!</h1></body>" +
                "</html>";
    }

    @RequestMapping(path = "/eval", method = RequestMethod.POST, consumes = "*/*;charset=UTF-8", produces = "text/plain")
    public String eval(Authentication authentication, @RequestBody String expression) throws ParsingException {
        try {
            LOG.debug("Evaluation request: [" + expression + "]");
            int begin = 0;
            int end = 0;
            boolean isIn = false;
            for (int i = 0; i < expression.length(); ++i) {
                if (Character.isLetter(expression.charAt(i)) && !isIn) {
                    begin = i;
                    isIn = true;
                    continue;
                }
                if ((Character.isLetter(expression.charAt(i)) || Character.isDigit(expression.charAt(i))) && isIn) {
                    end = i;
                    continue;
                }
                if (!Character.isLetter(expression.charAt(i)) && isIn) {
                    isIn = false;
                    String var = expression.substring(begin, end + 1);
                    if(FUNCTIONS.contains(var)) {
                        continue;
                    }
                    String value = billingDao.getValue(authentication.getName(), var);
                    expression = expression.replaceFirst(var, value);
                    i = 0;
                }
            }
            double result = calculator.calculate(expression);
            LOG.trace("Result: " + result);
            return Double.toString(result) + "\n";
        } catch (ParsingException e) {
            return e.getMessage();
        }
    }

    @RequestMapping(path = "/addUser", method = RequestMethod.POST, consumes = "*/*;charset=UTF-8")
    public void addUser(@RequestBody String nameAndPass) {
        String[] data = nameAndPass.split(" ");
        LOG.debug("Start adding new user [ " + data[0] + ", " + data[1] + " ]");
        billingDao.addUser(data[0], data[1]);
        LOG.trace("Added new user");
    }
    @RequestMapping(path = "/variable/{varName}", method = RequestMethod.PUT, consumes = "*/*;charset=UTF-8", produces = "text/plain")
    public String  addVariable(Authentication authentication, @PathVariable String varName, @RequestBody String argument) {
        try {
            String ourName = authentication.getName();
            LOG.debug("Start adding new variable [" + varName + "] as " + argument + " for " + ourName);
            String value = eval(authentication, argument);
            billingDao.addVariable(ourName, varName, Double.valueOf(value));
            LOG.trace("Added new variable");
            return "OK";
        } catch (ParsingException e) {
            return e.getMessage();
        }
    }
    @RequestMapping(path = "/variable/{varName}", method = RequestMethod.DELETE)
    public void deleteVariable(Authentication authentication, @PathVariable String varName) {
        String ourName = authentication.getName();
        LOG.debug("Start deleting variable [" + varName + "] from [" + ourName + "]");
        billingDao.deleteVariable(ourName, varName);
        LOG.trace("Deleted variable");
    }
    @RequestMapping(path = "/variable/", method = RequestMethod.GET, produces = "text/plain")
    public String getVariables(Authentication authentication) {
        LOG.debug("Start getting variables value from [" + authentication.getName() + "]");
        HashMap<String, String> ans = billingDao.getVariables(authentication.getName());
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String, String> entry : ans.entrySet()) {
            sb.append(entry.getKey());
            sb.append(" = ");
            sb.append(entry.getValue());
            sb.append(";");
        }
        LOG.trace("Got variables");
        return sb.toString();
    }
    @RequestMapping(path = "/variable/{varName}", method = RequestMethod.GET, produces = "text/plain")
    public String getVariableValue(Authentication authentication, @PathVariable String varName) {
        try {
            String ourName = authentication.getName();
            LOG.debug("Start getting variable value [" + varName + "] from [" + ourName + "]");
            String ans = billingDao.getValue(ourName, varName);
            LOG.trace("Got value");
            return ans;
        } catch (ParsingException e) {
            return e.getMessage();
        }
    }
}
