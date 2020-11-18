import React from "react";
import { HashRouter, Route, Switch, Redirect } from "react-router-dom";
import Dashboard from "./components/Dashboard";
import HealthCheck from "./components/HealthCheck";
import ServerInfo from "./components/ServerInfo";
import Login from "./components/Login";
import Header from './components/Header';

const App = () => {
  return (
    <HashRouter>
      <Header/>
      <Switch>
        <Route exact path="/" component={Dashboard} />
        <Route path="/check" component={HealthCheck} />
        <Route path="/info" component={ServerInfo} />
        <Route path="/login" component={Login} />
      </Switch>
    </HashRouter>
  );
}

export default App;
