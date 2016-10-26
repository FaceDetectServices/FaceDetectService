package com.yoshio3.services;

/**
 * Created by yoterada on 2016/10/26.
 */
import com.yoshio3.services.entities.FaceDetectRequestJSONBody;
import com.yoshio3.services.entities.FaceDetectResponseJSONBody;
import com.yoshio3.services.entities.utils.MyObjectMapperProvider;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 *
 * @author Yoshio Terada
 */
@Component
@Path("")
public class FaceDetectService implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(FaceDetectService.class.getName());
    private static final String BASE_URI =
            "https://api.projectoxford.ai/face/v1.0/detect?returnFaceId=true" +
                    "&returnFaceLandmarks=false&returnFaceAttributes=age,gender";

    @Value("${vcap.services.facedetect-service.credentials.subscriptionId:デフォルト値}")
    String subscriptionId;

    @GET
    @Path("/facedetect")
    @Produces("application/json")
    //public String getFaceDetectInfo(){
    public String getFaceDetectInfo(@QueryParam("url") String fileURL){
        try {
            //String fileURL="https%3A%2F%2Fyoshiofileup.blob.core.windows.net%2Fuploaded%2F5a0cbbcf-5645-4945-bf76-488cae3ecdfc.jpg";
            System.out.println(fileURL);
            String file = URLDecoder.decode(fileURL,"UTF-8");
            Future<Response>  futureResponse = getFaceInfo(file);
            Response response = futureResponse.get();

            return jobForFace(response);
        } catch (InterruptedException |ExecutionException | UnsupportedEncodingException e) {
           e.printStackTrace();
        }
        return "";
    }

    /*
        対応ォーマット： JPEG, PNG, GIF(最初のフレーム), BMP
        画像サイズ： 4MB 以下
        検知可能な顔のサイズ：36x36 〜 4096x4096
        一画像辺り検知可能な人数：64 名
        指定可能な属性オプション(まだ実験的不正確)：
            age, gender, headPose, smile and facialHair, and glasses
            HeadPose の pitch 値は 0 としてリザーブ
     */

    public Future<Response> getFaceInfo(String pictURI)
            throws InterruptedException, ExecutionException {
        Client client = ClientBuilder.newBuilder()
                .register(MyObjectMapperProvider.class)
                .register(JacksonFeature.class)
                .build();

        WebTarget target = client.target(BASE_URI);
        FaceDetectRequestJSONBody entity = new FaceDetectRequestJSONBody();
        entity.setUrl(pictURI);

        Future<Response> response = target
                .request(MediaType.APPLICATION_JSON)
                .header("Ocp-Apim-Subscription-Key", subscriptionId)
                .async()
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));
        return response;
    }

    private String jobForFace(Response faceRes) {
        FaceDetectResponseJSONBody[] persons = null;
        if (checkRequestSuccess(faceRes)) {
            persons = faceRes.readEntity(FaceDetectResponseJSONBody[].class);
        } else {
            return faceRes.readEntity(String.class);
        }
        //現在は一人のみ解析処理
        FaceDetectResponseJSONBody faceDetectData = persons[0];

        //年齢、性別を取得
        Map<String, Object> faceAttributes = faceDetectData.getFaceAttributes();
        Double age = (Double) faceAttributes.get("age");
        String gender = (String) faceAttributes.get("gender");
        return "{\"age\":" + age + ",\"gender\":\"" + gender +"\"}";
    }

    /*
     REST 呼び出し成功か否かの判定
     */
    protected boolean checkRequestSuccess(Response response) {
        Response.StatusType statusInfo = response.getStatusInfo();
        Response.Status.Family family = statusInfo.getFamily();
        return family != null && family == Response.Status.Family.SUCCESSFUL;
    }
}

