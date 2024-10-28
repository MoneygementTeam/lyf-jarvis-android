// API 응답을 위한 데이터 클래스들
data class ChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val usage: Usage,
    val choices: List<Choice>
) {
    // content를 쉽게 가져오기 위한 확장 함수
    fun getFirstContent(): String {
        return choices.firstOrNull()?.message?.content?.trim() ?: ""
    }
}

data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
    val completionTokensDetails: CompletionTokensDetails
)

data class CompletionTokensDetails(
    val reasoningTokens: Int
)

data class Choice(
    val message: Message,
    val logprobs: String?,
    val finishReason: String,
    val index: Int
)