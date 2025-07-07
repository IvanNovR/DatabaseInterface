module ru.ivannovr.dbinterface {
    requires javafx.controls;
    requires javafx.fxml;
    requires org.controlsfx.controls;
    requires org.apache.logging.log4j;
    requires java.sql;

    opens ru.ivannovr.dbinterface to javafx.fxml;
    opens ru.ivannovr.dbinterface.ui to javafx.fxml;
    opens ru.ivannovr.dbinterface.utils to javafx.fxml;
    exports ru.ivannovr.dbinterface;
    exports ru.ivannovr.dbinterface.ui;
    exports ru.ivannovr.dbinterface.utils;
}