import React from "react";
import { BrowserRouter, Route, Switch } from "react-router-dom";
import Dashboard from "./components/Dashboard";
import HealthCheck from "./components/HealthCheck";
import ServerInfo from "./components/ServerInfo";
import LoggerConfig from "./components/LoggerConfig";
import Login from "./components/Login";
import Header from './components/Header';
import Form from "./components/Form";
import Failure from "./components/Failure";
import Success from "./components/Success";

const App = () => {
  return (
    <BrowserRouter>
      <Header/>
      <Switch>
        <Route exact path="/" component={Dashboard} />
        <Route exact path="/form/:formId" component={Form} />
        <Route path="/check" component={HealthCheck} />
        <Route path="/info" component={ServerInfo} />
        <Route path="/login" component={Login} />
        <Route path="/logger" component={LoggerConfig} />
        <Route path="/failure" component={Failure} />
        <Route path="/success" component={Success} />
      </Switch>
    </BrowserRouter>
  );
}

export default App;
