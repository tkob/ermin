package yokohama.lang.ermin.ant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import lombok.Setter;
import yokohama.lang.ermin.back.reladomo.ReladomoTranslator;
import yokohama.lang.ermin.front.ErminTuple;
import yokohama.lang.ermin.front.FrontEndProcessor;
import yokohama.lang.reladomo.MithraObjectResourceType;
import yokohama.lang.reladomo.MithraObjectType;
import yokohama.lang.reladomo.MithraType;
import yokohama.lang.reladomo.ObjectFactory;

public class Reladomo extends Task {

    final FrontEndProcessor frontEndProcessor = new FrontEndProcessor();

    final ReladomoTranslator reladomoTranslator = new ReladomoTranslator();

    final ObjectFactory factory = new ObjectFactory();

    @Setter
    private File source;

    @Setter
    private File destination;

    @Setter
    private String manifest = "ReladomoClassList.xml";

    @Override
    public void execute() {
        if (source == null) {
            throw new BuildException("source is not set");
        }
        if (destination == null) {
            throw new BuildException("destination is not set");
        }

        try {
            final ErminTuple erminTuple = frontEndProcessor.process(
                    new FileInputStream(source));

            final Iterable<MithraObjectType> mithraObjects = reladomoTranslator
                    .toMithraObjects(erminTuple);

            // Create the Reladomo object XML files
            {
                final JAXBContext context = JAXBContext.newInstance(
                        MithraObjectType.class);
                final Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                for (final MithraObjectType mithraObject : mithraObjects) {
                    try (final OutputStream os = new FileOutputStream(new File(destination, mithraObject
                            .getClassName() + ".xml"))) {
                        marshaller.marshal(factory.createMithraObject(mithraObject), os);
                    }
                }
            }

            // Create the Reladomo class list
            {
                final JAXBContext context = JAXBContext.newInstance(MithraType.class);
                final Marshaller marshaller = context.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                final MithraType mithra = factory.createMithraType();
                final List<MithraObjectResourceType> mithraObjectResources = mithra
                        .getMithraObjectResource();
                for (final MithraObjectType mithraObject : mithraObjects) {
                    final MithraObjectResourceType mithraObjectResource = factory
                            .createMithraObjectResourceType();
                    mithraObjectResource.setName(mithraObject.getClassName());
                    mithraObjectResources.add(mithraObjectResource);
                }
                try (final OutputStream os = new FileOutputStream(new File(destination, manifest))) {
                    marshaller.marshal(factory.createMithra(mithra), os);
                }
            }

        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

}
