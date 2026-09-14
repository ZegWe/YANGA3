package com.yanga.client.api
import org.junit.Test
class NgaSignatureParserTest {
  @Test fun acceptsConfirmedSave() { NgaSignatureParser.requireSuccess("""{"data":{"0":"操作成功"}}""") }
  @Test(expected = NgaApiException::class) fun rejectsUnconfirmedSave() { NgaSignatureParser.requireSuccess("<html>登录</html>") }
  @Test(expected = NgaApiException::class) fun errorOverridesMessage() { NgaSignatureParser.requireSuccess("""{"error":"失败","data":{"0":"操作成功"}}""") }
}
