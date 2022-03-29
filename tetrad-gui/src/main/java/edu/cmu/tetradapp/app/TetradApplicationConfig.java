///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015 by Peter Spirtes, Richard Scheines, Joseph   //
// Ramsey, and Clark Glymour.                                                //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////
package edu.cmu.tetradapp.app;

import edu.cmu.tetrad.session.SessionNode;
import edu.cmu.tetrad.util.DefaultTetradLoggerConfig;
import edu.cmu.tetrad.util.TetradLogger;
import edu.cmu.tetrad.util.TetradLoggerConfig;
import edu.cmu.tetradapp.editor.ParameterEditor;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.*;

/**
 * Represents the configuration details for the Tetrad application.
 *
 * @author Tyler Gibson
 */
public class TetradApplicationConfig {

    /**
     * The singleton instance
     */
    private static final TetradApplicationConfig instance = new TetradApplicationConfig();

    private final Map<String, SessionNodeConfig> configs;

    /**
     * A map from model classes to the configurations that handle them.
     */
    private final Map<Class, SessionNodeConfig> classMap = new HashMap<>();

    /**
     * Constructs the configuration.
     */
    private TetradApplicationConfig() {
        // Tetrad-Gui properties file, use absolute path with leading "/"
        final InputStream tetradGuiPropertiesStream = this.getClass().getResourceAsStream("/tetrad-gui.properties");

        final Properties tetradGuiProperties = new Properties();

        try {
            tetradGuiProperties.load(tetradGuiPropertiesStream);
        } catch (final IOException ex) {
            throw new IllegalStateException("Could not load tetrad-gui.properties file", ex);
        }

        try {
            tetradGuiPropertiesStream.close();
        } catch (final IOException ex) {
            throw new IllegalStateException("Could not close the tetradGuiPropertiesStream", ex);
        }

        // Load different config xml files based config setting - development or production- Zhou
        final String configXml = tetradGuiProperties.getProperty("tetrad-gui.config");

        System.out.println("config file: " + configXml);

        final InputStream stream = this.getClass().getResourceAsStream(configXml);
        final Builder builder = new Builder(true);
        try {
            final Document doc = builder.build(stream);
            this.configs = TetradApplicationConfig.buildConfiguration(doc.getRootElement());
            for (final SessionNodeConfig config : this.configs.values()) {
                final Class[] models = config.getModels();
                for (final Class model : models) {
                    if (this.classMap.containsKey(model)) {
                        throw new IllegalStateException("Model " + model + " has two configurations");
                    }
                    this.classMap.put(model, config);
                }
            }
        } catch (final Exception ex) {
            throw new IllegalStateException("Could not load configuration", ex);
        }
    }

    //============================== Public Methods =====================================//

    /**
     * @return an instance of the session configuration.
     */
    public static TetradApplicationConfig getInstance() {
        return TetradApplicationConfig.instance;
    }

    /**
     * @param id - The id of the session config (e.g., "Graph" etc)
     * @return the <code>SessionNodeConfig</code> to be used for the given id,
     * or null if there isn't one defined for the given id.
     */
    public SessionNodeConfig getSessionNodeConfig(final String id) {
        return this.configs.get(id);
    }

    /**
     * @return the <code>SessionNodeConfig</code> that the given model is part
     * of.
     */
    public SessionNodeConfig getSessionNodeConfig(final Class model) {
        return this.classMap.get(model);
    }

    //============================== Private Methods ====================================//

    /**
     * Loads the configuration from the root element of the configuratin.xml
     * file. It is assumed that the document has been validated against its dtd
     * already.
     */
    private static Map<String, SessionNodeConfig> buildConfiguration(final Element root) {
        final Elements elements = root.getChildElements();
        final ClassLoader loader = TetradApplicationConfig.getClassLoader();
        final Map<String, SessionNodeConfig> configs = new LinkedHashMap<>();
        for (int i = 0; i < elements.size(); i++) {
            final Element node = elements.get(i);
            final String id = node.getAttributeValue("id");
            final DefaultNodeConfig nodeConfig = new DefaultNodeConfig(id);
            final Elements nodeElements = node.getChildElements();
            for (int k = 0; k < nodeElements.size(); k++) {
                final Element child = nodeElements.get(k);
                if ("models".equals(child.getQualifiedName())) {
                    nodeConfig.setSessionNodeModelConfig(TetradApplicationConfig.buildModelConfigs(child));
                } else if ("display-component".equals(child.getQualifiedName())) {
                    final String image = child.getAttributeValue("image");
                    final String value = TetradApplicationConfig.getValue(child);
                    final Class compClass = value == null ? null : TetradApplicationConfig.loadClass(loader, value);
                    nodeConfig.setDisplayComp(image, compClass);
                } else if ("model-chooser".equals(child.getQualifiedName())) {
                    final String title = child.getAttributeValue("title");
                    final String value = TetradApplicationConfig.getValue(child);
                    final Class chooserClass = value == null ? null : TetradApplicationConfig.loadClass(loader, value);
                    nodeConfig.setChooser(title, chooserClass);
                } else if ("node-specific-message".equals(child.getQualifiedName())) {
                    nodeConfig.setNodeSpecificMessage(child.getValue());
                } else {
                    throw new IllegalStateException("Unknown element " + child.getQualifiedName());
                }
                configs.put(id, nodeConfig);
            }
        }
        return configs;
    }

    /**
     * @return the value of the elemnt, will return null if its an empty string.
     */
    private static String getValue(final Element value) {
        final String v = value.getValue();
        if (v != null && v.length() == 0) {
            return null;
        }
        return v;
    }

    /**
     * Builds the model configs from the models element.
     */
    private static List<SessionNodeModelConfig> buildModelConfigs(final Element models) {
        final Elements modelElements = models.getChildElements();
        final List<SessionNodeModelConfig> configs = new LinkedList<>();
        final ClassLoader loader = TetradApplicationConfig.getClassLoader();
        for (int i = 0; i < modelElements.size(); i++) {
            final Element model = modelElements.get(i);
            final String name = model.getAttributeValue("name");
            final String acronym = model.getAttributeValue("acronym");
            final String help = model.getAttributeValue("help");
            final String category = model.getAttributeValue("category");
            Class modelClass = null;
            Class editorClass = null;
            final Class paramsClass = null;
            Class paramsEditorClass = null;
            TetradLoggerConfig loggerConfig = null;
            final Elements elements = model.getChildElements();
            for (int k = 0; k < elements.size(); k++) {
                final Element element = elements.get(k);
                if ("model-class".equals(element.getQualifiedName())) {
                    modelClass = TetradApplicationConfig.loadClass(loader, element.getValue());
                } else if ("editor-class".equals(element.getQualifiedName())) {
                    editorClass = TetradApplicationConfig.loadClass(loader, element.getValue());
//                } else if ("params-class".equals(element.getQualifiedName())) {
//                    paramsClass = loadClass(loader, element.getValue());
                } else if ("params-editor-class".equals(element.getQualifiedName())) {
                    paramsEditorClass = TetradApplicationConfig.loadClass(loader, element.getValue());
                } else if ("logger".equals(element.getQualifiedName())) {
                    loggerConfig = TetradApplicationConfig.configureLogger(element);
                } else {
                    throw new IllegalStateException("Unknown element: " + element.getQualifiedName());
                }
            }
            // if there is a logger config, add it with its model to the tetrad logger.
            if (loggerConfig != null) {
                TetradLogger.getInstance().addTetradLoggerConfig(modelClass, loggerConfig);
            }

            final SessionNodeModelConfig config = new DefaultModelConfig(modelClass, paramsClass,
                    paramsEditorClass, editorClass, name, acronym, help, category);
            configs.add(config);
        }
        return configs;
    }

    /**
     * Configures the logger that the given element represents and returns its
     * id.
     */
    private static TetradLoggerConfig configureLogger(final Element logger) {
        final Elements elements = logger.getChildElements();
        final List<TetradLoggerConfig.Event> events = new LinkedList<>();
        final List<String> defaultLog = new LinkedList<>();
        for (int i = 0; i < elements.size(); i++) {
            final Element event = elements.get(i);
            final String eventId = event.getAttributeValue("id");
            final String description = event.getAttributeValue("description");
            final String defaultOption = event.getAttributeValue("default");
            if ("on".equals(defaultOption)) {
                defaultLog.add(eventId);
            }
            events.add(new DefaultTetradLoggerConfig.DefaultEvent(eventId, description));
        }
        final TetradLoggerConfig config = new DefaultTetradLoggerConfig(events);
        // set any defaults
        for (final String event : defaultLog) {
            config.setEventActive(event, true);
        }
        return config;
    }

    /**
     * Creates the display comp from an image/comp class. If the not null then
     * it is given as an argument to the constructor of the given class. IF the
     * givne comp is null then the default is used.
     */
    private static SessionDisplayComp createDisplayComp(final String image, final Class compClass) {
        if (compClass == null) {
            return new StdDisplayComp(image);
        }
        try {
            if (image == null) {
                return (SessionDisplayComp) compClass.newInstance();
            }
            final Constructor constructor = compClass.getConstructor(String.class);
            return (SessionDisplayComp) constructor.newInstance(image);
        } catch (final Exception ex) {
            throw new IllegalStateException("Could not create display component", ex);
        }
    }

    private static Class loadClass(final ClassLoader loader, final String className) {
        try {
            return loader.loadClass(className.trim());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("The class name " + className + " could not be found", e);
        }
    }

    /**
     * @return a class loader to use.
     */
    private static ClassLoader getClassLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = TetradApplicationConfig.class.getClassLoader();
        }
        // if its still null nothing we can do.
        if (loader == null) {
            throw new NullPointerException("Class loader was null could not load handler");
        }
        return loader;
    }

    /**
     * removes newline and extra white space (Seems to be sensitive to this,
     * when its html)
     */
    private static String pruneNodeSpecificMessage(final String text) {
        final int size = text.length();
        int i = 0;
        final StringBuilder builder = new StringBuilder(size);
        while (i < size) {
            char c = text.charAt(i);
            if (c == ' ') {
                builder.append(' ');
                // skip until non whitespace is found
                while (i < size - 1 && c == ' ') {
                    i++;
                    c = text.charAt(i);
                }
            }
            if (c != '\n') {
                builder.append(c);
            }
            i++;
        }
        return builder.toString().trim();
    }

    private static boolean matches(final Class[] params, final Object[] arguments) {
        if (params.length != arguments.length) {
            return false;
        }

        for (int i = 0; i < params.length; i++) {
            final Class param = params[i];
            if (!param.isInstance(arguments[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * A map from ids to node configs.
     */
    public Map<String, SessionNodeConfig> getConfigs() {
        return this.configs;
    }

    //============================== Inner classes =======================================//

    /**
     * Default implementation of the session config. Most functionality is
     * implemented by static methods from the outer-class.
     */
    private static class DefaultNodeConfig implements SessionNodeConfig {

        /**
         * ALl the config info of the configuration.
         */
        private final Map<Class, SessionNodeModelConfig> modelMap = new HashMap<>();
        private List<SessionNodeModelConfig> models;
        private String image;
        private Class compClass;
        private String nodeSpecificMessage;
        private final String id;
        private String chooserTitle;
        private Class chooserClass;

        public DefaultNodeConfig(final String id) {
            if (id == null) {
                throw new NullPointerException("The given id must not be null");
            }
            this.id = id;
        }

        public SessionNodeModelConfig getModelConfig(final Class model) {
            return this.modelMap.get(model);
        }

        public Class[] getModels() {
            final Class[] modelClasses = new Class[this.models.size()];
            for (int i = 0; i < this.models.size(); i++) {
                modelClasses[i] = this.models.get(i).getModel();
            }
            return modelClasses;
        }

        public String getNodeSpecificMessage() {
            return this.nodeSpecificMessage;
        }

        public ModelChooser getModelChooserInstance(final SessionNode sessionNode) {
            final ModelChooser chooser;
            if (this.chooserClass == null) {
                chooser = new DefaultModelChooser();
            } else {
                try {
                    chooser = (ModelChooser) this.chooserClass.newInstance();
                    chooser.setSessionNode(sessionNode);
                } catch (final InstantiationException e) {
                    throw new IllegalStateException("Model chooser must have empty constructor", e);
                } catch (final IllegalAccessException e) {
                    throw new IllegalStateException("Model chooser must have empty constructor", e);
                }
            }

            final Class[] consistentClasses = sessionNode.getConsistentModelClasses(false);

            final List<SessionNodeModelConfig> filteredModels = new ArrayList<>();

            for (final SessionNodeModelConfig config : this.models) {
                final Class clazz = config.getModel();

                boolean exists = false;

                for (final Class clazz2 : consistentClasses) {
                    if (clazz.equals(clazz2)) {
                        exists = true;
                        break;
                    }
                }

                if (exists) {
                    filteredModels.add(config);
                }
            }

            chooser.setSessionNode(sessionNode);
            chooser.setNodeId(this.id);
            chooser.setTitle(this.chooserTitle);
//            chooser.setNodeName(sessionNode.getDisplayName());
//            chooser.setModelConfigs(new ArrayList<SessionNodeModelConfig>(this.models));
            chooser.setModelConfigs(new ArrayList<>(filteredModels));
            chooser.setup();
            return chooser;
        }

        public SessionDisplayComp getSessionDisplayCompInstance() {
            return TetradApplicationConfig.createDisplayComp(this.image, this.compClass);
        }

        //========================= Private Methods ===============================//
        private void setChooser(final String title, final Class chooserClass) {
            if (title == null) {
                throw new NullPointerException("The chooser title must not be null");
            }
            this.chooserTitle = title;
            this.chooserClass = chooserClass;
        }

        private void setNodeSpecificMessage(final String text) {
            if (text == null) {
                throw new NullPointerException("The node specific message text must not be null");
            }
            this.nodeSpecificMessage = TetradApplicationConfig.pruneNodeSpecificMessage(text);
        }

        private void setDisplayComp(final String image, final Class comp) {
            if (image == null && comp == null) {
                throw new NullPointerException("Must have an image or a display component class defined");
            }
            this.image = image;
            this.compClass = comp;
        }

        private void setSessionNodeModelConfig(final List<SessionNodeModelConfig> configs) {
            this.models = configs;
            for (final SessionNodeModelConfig config : configs) {
                this.modelMap.put(config.getModel(), config);
            }
        }
    }

    /**
     * THe default implementation of the model config.
     */
    private static class DefaultModelConfig implements SessionNodeModelConfig {

        private final Class model;
        private final Class params;
        private final Class paramsEditor;
        private final Class editor;
        private final String name;
        private final String acronym;
        private final String help;
        private final String category;

        public DefaultModelConfig(final Class model, final Class params, final Class paramsEditor, final Class editor,
                                  final String name, final String acronym, final String help, final String category
        ) {
            if (model == null || editor == null || name == null || acronym == null) {
                throw new NullPointerException("Values must not be null");
            }
            this.model = model;
            this.params = params;
            this.paramsEditor = paramsEditor;
            this.editor = editor;
            this.name = name;
            this.help = help;
            this.acronym = acronym;
            this.category = category;
        }

        public String getHelpIdentifier() {
            return this.help;
        }

        public String getCategory() {
            return this.category;
        }

        public Class getModel() {
            return this.model;
        }

        public String getName() {
            return this.name;
        }

        public String getAcronym() {
            return this.acronym;
        }

        public JPanel getEditorInstance(final Object[] arguments) {
            final Class[] parameters = new Class[arguments.length];

            for (int i = 0; i < arguments.length; i++) {
                parameters[i] = arguments[i].getClass();
            }

            Constructor constructor = null;

            try {
                constructor = this.editor.getConstructor(parameters);
            } catch (final Exception ex) {
                // do nothing, try to find a constructor below.
            }

            if (constructor == null) {
                // try to find object-compatable constructor.
                final Constructor[] constructors = this.editor.getConstructors();
                for (final Constructor _constructor : constructors) {
                    final Class[] params = _constructor.getParameterTypes();
                    if (TetradApplicationConfig.matches(params, arguments)) {
                        constructor = _constructor;
                        break;
                    }
                }
            }

            if (constructor == null) {
                throw new NullPointerException("Could not find constructor in " + this.editor + " for model: " + this.model);
            }

            try {
                return (JPanel) constructor.newInstance(arguments);
            } catch (final Exception ex) {
                throw new IllegalStateException("Could not construct editor", ex);
            }
        }

        //        public Parameters getParametersInstance() {
//            if (this.params != null) {
//                try {
//                    return (Parameters) this.params.newInstance();
//                }
//                catch (ClassCastException e) {
//                    throw new IllegalStateException("Model params doesn't implement Parameters", e);
//                }
//                catch (Exception e) {
//                    throw new IllegalStateException("Error instantiating params, must be empty constructor", e);
//                }
//            }
//            return null;
//        }
        public ParameterEditor getParameterEditorInstance() {
            if (this.paramsEditor != null) {
                try {
                    return (ParameterEditor) this.paramsEditor.newInstance();
                } catch (final ClassCastException e) {
                    throw new IllegalStateException("Parameters editor must implement ParameterEditor", e);
                } catch (final Exception e) {
                    throw new IllegalStateException("Error intatiating params editor, must have empty constructor", e);
                }
            }
            return null;
        }

    }

}
