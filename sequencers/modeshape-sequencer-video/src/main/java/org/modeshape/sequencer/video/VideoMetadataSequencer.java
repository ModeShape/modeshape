/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.video;

import static org.modeshape.sequencer.video.VideoMetadataLexicon.BITRATE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.CHANNELS;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.CODEC;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.COMMENT;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.DURATION;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.ENCODER;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.FRAMERATE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.HEIGHT;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.METADATA_NODE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.SAMPLERATE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.STREAM_NODE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.STREAM_TYPE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.TITLE;
import static org.modeshape.sequencer.video.VideoMetadataLexicon.WIDTH;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;

/**
 * A sequencer that processes the binary content of a video file, extracts the metadata, and then writes that
 * metadata to the repository.
 * <p>
 * This sequencer produces data that corresponds to the following structure:
 * <ul>
 * <li><strong>video:metadata</strong> node of type <code>video:metadata</code>
 * <ul>
 * <li><strong>jcr:mimeType</strong> - optional string property for the mime type of the video</li>
 * <li><strong>video:duration</strong> - optional double property specifying the length of the video</li>
 * <li><strong>video:bitrate</strong> - optional long property specifying the bitrate of the video</li>
 * <li><strong>video:title</strong> - optional string property for the title of the video</li>
 * <li><strong>video:comment</strong> - optional string property for the comment of the video</li>
 * <li><strong>video:encoder</strong> - optional string property specifying the encoder</li>
 * <li><strong>video:stream</strong> - optional child not which contains metadata of the stream</li>
 * <ul>
 * <li><strong>video:streamType</strong> - optional string property which specifies type of stream ('audio', 'video', 'unknown')</li>
 * <li><strong>video:codec</strong> - optional string property specifying the codec of the stream</li>
 * <li><strong>video:bitrate</strong> - optional long property specifying the stream's bitrate</li>
 * <li><strong>video:framerate</strong> - optional double property specifying the stream's framerate</li>
 * <li><strong>video:samplerate</strong> - optional long property specifying the stream's samplerate</li>
 * <li><strong>video:channels</strong> - optional long property specifying the number of audio stream's channels</li>
 * <li><strong>video:width</strong> - optional long property specifying the video stream's width</li>
 * <li><strong>video:height</strong> - optional long property specifying the video stream's height</li>
 * </ul>
 * </ul>
 * </li>
 * </ul>
 * </p>
 * 
 * @since 5.1
 */
public class VideoMetadataSequencer extends Sequencer {

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("video.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(VideoMetadata.MIME_TYPE_STRINGS);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = (Binary) inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        String mimeType = binaryValue.getMimeType();

        Node sequencedNode = getMetadataNode(outputNode);
        setPropertyIfMetadataPresent(sequencedNode, JcrConstants.JCR_MIME_TYPE, mimeType);
        return processBasicMetadata(sequencedNode, binaryValue);
    }

    private boolean processBasicMetadata( Node sequencedNode,
                                          Binary binaryValue ) {
        VideoMetadata metadata = null;
        try (InputStream stream = binaryValue.getStream()) {
            metadata = new VideoMetadata(stream);
            if (metadata.check()) {
                setPropertyIfMetadataPresent(sequencedNode, DURATION, metadata.getDuration());
                setPropertyIfMetadataPresent(sequencedNode, BITRATE, metadata.getBitrate());
                setPropertyIfMetadataPresent(sequencedNode, TITLE, metadata.getTitle());
                setPropertyIfMetadataPresent(sequencedNode, COMMENT, metadata.getComment());
                setPropertyIfMetadataPresent(sequencedNode, ENCODER, metadata.getEncoder());

                int suffix = 0;
                for (StreamMetadata streamMetadata : metadata.getStreams()) {
                    Node streamNode = sequencedNode.addNode(STREAM_NODE + String.valueOf(suffix), STREAM_NODE);
                    processStreamMetadata(streamNode, streamMetadata);
                    suffix += 1;
                }
                return true;
            }
        } catch (Exception e) {
            getLogger().error(e, "Couldn't process the stream.");
        }
        return false;
    }

    private boolean processStreamMetadata( Node streamNode,
                                           StreamMetadata stream) throws RepositoryException {
        setPropertyIfMetadataPresent(streamNode, STREAM_TYPE, stream.getStreamType());
        setPropertyIfMetadataPresent(streamNode, CODEC, stream.getCodec());
        setPropertyIfMetadataPresent(streamNode, BITRATE, stream.getBitrate());
        setPropertyIfMetadataPresent(streamNode, FRAMERATE, stream.getFramerate());
        setPropertyIfMetadataPresent(streamNode, SAMPLERATE, stream.getSamplerate());
        setPropertyIfMetadataPresent(streamNode, CHANNELS, stream.getChannels());
        setPropertyIfMetadataPresent(streamNode, WIDTH, stream.getWidth());
        setPropertyIfMetadataPresent(streamNode, HEIGHT, stream.getHeight());

        return true;
    }

    private Node getMetadataNode( Node outputNode ) throws RepositoryException {
        if (outputNode.isNew()) {
            outputNode.setPrimaryType(METADATA_NODE);
            return outputNode;
        }
        return outputNode.addNode(METADATA_NODE, METADATA_NODE);
    }

    private void setPropertyIfMetadataPresent( Node node,
                                               String propertyName,
                                               Object value ) throws RepositoryException {
        if (value == null) {
            return;
        }
            if (value instanceof String && !StringUtil.isBlank((String) value)) {
                node.setProperty(propertyName, (String) value);
            } else if (value instanceof Boolean) {
                node.setProperty(propertyName, (Boolean) value);
            } else if (value instanceof Double) {
                node.setProperty(propertyName, (Double) value);
            } else if (value instanceof Number) {
                node.setProperty(propertyName, ((Number) value).longValue());
            } else if (value instanceof Calendar) {
                node.setProperty(propertyName, (Calendar) value); 
            } else if (value instanceof byte[]) {
                InputStream is = new ByteArrayInputStream((byte []) value);
                javax.jcr.Binary binaryProperty = node.getSession().getValueFactory().createBinary(is);
                node.setProperty(propertyName, binaryProperty);
            } else if (value instanceof List) {
                ValueFactory vf = node.getSession().getValueFactory();
                List<Value> values = ((List<?>) value).stream()
                                                      .filter(val -> val instanceof String)
                                                      .map(val -> vf.createValue((String) val))
                                                      .collect(Collectors.toList());
                if (!values.isEmpty()) {
                    node.setProperty(propertyName, values.toArray(new Value[values.size()]));
                }
            } else {
                getLogger().warn("The value of the property {0} has unknown type and couldn't be saved.", propertyName);
            }
    }
}
