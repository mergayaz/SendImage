package kz.kuz.sendimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import javax.net.ssl.HttpsURLConnection

// В данном упражнении загружаем изображение с телефона на сервер
class MainActivity : AppCompatActivity() {
    private lateinit var picture: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        picture = findViewById(R.id.imageView)
        val pathToImage = applicationContext.filesDir.path + "/cumin.jpg"
        // это путь к изображению, которое будем загружать на сервер
        // изображение нельзя ложить в папку drawable, там меняется его размер
        val bitmap = BitmapFactory.decodeFile(pathToImage)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        val imgString = Base64.encodeToString(byteArray, Base64.URL_SAFE)
        // обязательно указываем Base64.URL_SAFE, иначе строка с закодированным изображение придёт
        // на сервер с ошибками
        // при указании данной метки в строке символ "/" меняется на "_", а "+" на "-"
        // соответственно, после получения строки на сервере нужно будет поменять их обратно
        // (внизу дан php код на сервере
        picture.setImageBitmap(bitmap)

        val executorService = Executors.newSingleThreadExecutor()
        executorService.submit {
            val url = URL("https://101.kz/create.php")
            val connection = url.openConnection() as HttpsURLConnection
            connection.doOutput = true // устанавливаем POST запрос
            val out: OutputStream = BufferedOutputStream(connection.outputStream)
            val writer = BufferedWriter(OutputStreamWriter(out))
            writer.write("imgString=")
            writer.write(imgString) // write можно указать вместе, вот так: "imgString=$imgString"
            writer.flush()
            writer.close()
            out.close()
            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                // без команды connection.responseCode данные не отправляются
                connection.disconnect()
            }
        }
        executorService.shutdown()
    }
}
// php код на сервере:
// <?php
// $data1 = $_POST['imgString'];
// $data2 = str_replace('_', '/', $data1);
// $data3 = str_replace('-', '+', $data2);
// $myfile = fopen("from_phone.jpg", "w");
// fwrite($myfile, base64_decode($data3));
// fclose($myfile);
// ?>