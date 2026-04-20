package kr.or.thejejachurch.api.ops

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path

class OpsMediaServingConfigContractTest {

    @Test
    fun `production compose should expose upload settings and bind mount to app service`() {
        val appService = serviceBlock(readDeployFile("docker-compose.prod.yml"), "app")

        assertThat(appService).containsPattern(uploadEnvPattern("TDCH_UPLOAD_ROOT"))
        assertThat(appService).containsPattern(uploadEnvPattern("TDCH_UPLOAD_PUBLIC_BASE_URL"))
        assertThat(appService).containsPattern("""(?m)^\s*-\s*/opt/tdch/uploads:/opt/tdch/uploads\s*$""")
        assertThat(appService).containsPattern("""(?m)^\s*user:\s*["']1000:1000["']\s*$""")
    }

    @Test
    fun `production env example should document upload serving settings`() {
        val content = Files.readString(Path.of(".env.production.example"))

        assertThat(content).contains("TDCH_UPLOAD_ROOT=/opt/tdch/uploads")
        assertThat(content).contains("TDCH_UPLOAD_PUBLIC_BASE_URL=https://api.tdch.co.kr/media")
    }

    @Test
    fun `nginx http context file should only define shared media rate limit and allowlist variables`() {
        val content = readNginxFile("api.tdch.co.kr.http.conf")

        assertThat(content).doesNotContainPattern("""(?m)^\s*server\s*\{""")
        assertThat(content).doesNotContainPattern("""(?m)^\s*location\s+/media/?\s*\{""")
        assertThat(content).containsPattern("""(?m)^\s*limit_req_zone\s+""")
        assertThat(content).containsPattern(
            """(?m)^\s*map\s+\${'$'}http_referer\s+\$[A-Za-z0-9_]*media[A-Za-z0-9_]*\s*\{""",
        )
        assertThat(content).containsPattern(
            """(?m)^\s*map\s+\${'$'}http_origin\s+\$[A-Za-z0-9_]*media[A-Za-z0-9_]*\s*\{""",
        )
    }

    @Test
    fun `nginx server context should serve media with upload limits and request guards`() {
        val content = readNginxFile("api.tdch.co.kr.conf")
        val mediaLocation = locationBlock(content, "/media/")

        assertThat(content).containsPattern("""(?m)^\s*client_max_body_size\s+12m\s*;""")
        assertThat(content).containsPattern("""(?m)^\s*include\s+(?:/etc/nginx/)?mime\.types\s*;""")
        assertThat(content).containsPattern("""(?m)^\s*default_type\s+application/octet-stream\s*;""")

        assertThat(mediaLocation).containsPattern("""(?m)^\s*alias\s+/opt/tdch/uploads/\s*;""")
        assertThat(mediaLocation).containsPattern("""(?m)^\s*limit_req\s+""")
        assertThat(mediaLocation).containsPattern("""(?m)^\s*try_files\s+""")
        assertThat(mediaLocation).containsPattern("""(?m)^\s*if\s+\(\s*\$[A-Za-z0-9_]*media[A-Za-z0-9_]*\s*=\s*0\s*\)\s*\{""")
        assertThat(mediaLocation).contains("\$http_referer")
        assertThat(mediaLocation).contains("\$http_origin")
    }

    @Test
    fun `media location should live only in nginx server context file`() {
        assertThat(readNginxFile("api.tdch.co.kr.http.conf"))
            .doesNotContainPattern("""(?m)^\s*location\s+/media/?\s*\{""")
        assertThat(readNginxFile("api.tdch.co.kr.conf"))
            .containsPattern("""(?m)^\s*location\s+/media/?\s*\{""")
    }

    private fun readDeployFile(fileName: String): String = Files.readString(Path.of("deploy", fileName))

    private fun readNginxFile(fileName: String): String = Files.readString(Path.of("deploy/nginx", fileName))

    private fun serviceBlock(content: String, serviceName: String): String {
        val pattern = Regex("""(?ms)^  $serviceName:\R(.*?)(?=^\S|\z)""")
        return pattern.find(content)?.value ?: throw AssertionError("Expected compose service '$serviceName' to exist")
    }

    private fun locationBlock(content: String, path: String): String {
        val pattern = Regex("""(?ms)^\s*location\s+${Regex.escape(path)}\s*\{(.*?)(?=^\s*}\s*$)""")
        return pattern.find(content)?.value ?: throw AssertionError("Expected nginx location '$path' to exist")
    }

    private fun uploadEnvPattern(name: String): String =
        """(?m)^\s*(?:-\s*)?$name\s*[:=]\s*\$\{$name(?::[^}]*)?}\s*$"""
}
