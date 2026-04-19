import java.awt.image.BufferedImage
import java.io.File
import java.util.Random
import javax.imageio.ImageIO

fun main() {
    val size = 256
    val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val random = Random()
    for (y in 0 until size) {
        for (x in 0 until size) {
            val v = random.nextInt(256)
            // very low alpha for subtle noise: alpha = 12 (out of 255)
            val color = (12 shl 24) or (v shl 16) or (v shl 8) or v
            img.setRGB(x, y, color)
        }
    }
    val dir = File("app/src/main/res/drawable-nodpi")
    dir.mkdirs()
    ImageIO.write(img, "png", File(dir, "noise_texture.png"))
}
