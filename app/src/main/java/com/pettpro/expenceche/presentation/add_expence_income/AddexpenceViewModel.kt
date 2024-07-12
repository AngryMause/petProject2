package com.pettpro.expenceche.presentation.add_expence_income

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pettpro.domain.add_expence_income.ActualTimeRepository
import com.pettpro.domain.add_expence_income.AddExpenceIncomeVerifyingRepository
import com.pettpro.domain.db.model.CategoryOfExpence
import com.pettpro.domain.db.model.CategoryOfIncome
import com.pettpro.domain.db.model.Expence
import com.pettpro.domain.db.model.Income
import com.pettpro.domain.db.model.User
import com.pettpro.domain.home.TypeOfContentInDashBoardTab
import com.pettpro.domain.login.LoginVerifyingRepository
import com.pettpro.domain.registration.FirebaseUsersRegistrationRepository
import com.pettpro.domain.registration.ToastControl
import com.pettpro.domain.usecases.userdb.UserDatabaseUseCases
import com.pettpro.expenceche.presentation.add_expence_income.model.AddExpenceDataState
import com.pettpro.expenceche.presentation.add_expence_income.model.AddExpenceIncomeFormEvent
import com.pettpro.expenceche.presentation.login.LoginViewModel
import com.pettpro.expenceche.presentation.login.model.LoginDataState
import com.pettpro.expenceche.presentation.login.model.LoginFormEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject


@HiltViewModel
class AddexpenceViewModel @Inject constructor(
    private val addExpenceIncomeVerifyingRepository: AddExpenceIncomeVerifyingRepository,
    private val firebaseUsersRegistrationRepository: FirebaseUsersRegistrationRepository,
    private val toastControl: ToastControl,
    private val userDatabaseUseCases: UserDatabaseUseCases,
    private val actualTimeRepository: ActualTimeRepository,
) : ViewModel() {
    private var isItIncome = false
    var state by mutableStateOf(AddExpenceDataState())
    var user by mutableStateOf(User())

    private val validationEventChannel = Channel<ValidationEvent>()
    val validationEvents = validationEventChannel.receiveAsFlow()

    init {
        Log.d("dfasfds", actualTimeRepository.getActualTime())
    }


    //its necessary to call this block code in custom function,not in init{}
    suspend fun initViewModel() {
        withContext(Dispatchers.Default) {
            user = userDatabaseUseCases.getUser()
        }
        Log.d("adsfsadfsa", user.toString())
    }

    fun onEvent(event: AddExpenceIncomeFormEvent) {
        when (event) {
            is AddExpenceIncomeFormEvent.AmountChange -> {
                state = state.copy(amount = event.amount)
            }

            is AddExpenceIncomeFormEvent.CategoryExpenceChange -> {
                state = state.copy(categoryOfExpence = setExpenceCaterogy(event.category))
            }

            is AddExpenceIncomeFormEvent.CategoryIncomeChange -> {

                state = state.copy(categoryOfIncome = setIncomeCaterogy(event.category))
            }
            is AddExpenceIncomeFormEvent.SetTypeOfMoneyFlow ->{
                if(event.type== TypeOfContentInDashBoardTab.Incomes){
                    isItIncome = true
                }else{
                    isItIncome = false
                }
            }
            is AddExpenceIncomeFormEvent.ShowToast -> {
                toastControl.show(event.text)
            }

            is AddExpenceIncomeFormEvent.Sumbit -> {
                submitData()
            }

            is AddExpenceIncomeFormEvent.Final -> {
                finalOfSaving()
            }

        }
    }

    private fun addIncome() {
        user.arrayOfIncomes.add(
            Income(
                (user.arrayOfIncomes.size).toString(),
                actualTimeRepository.getActualTime(),
                state.amount.toDouble(),
                state.categoryOfIncome!!.toString()
            )
        )
    }

    private fun addExpence() {
        user.arrayOfExpence.add(
            Expence(
                (user.arrayOfExpence.size).toString(),
                actualTimeRepository.getActualTime(),
                state.amount.toDouble(),
                state.categoryOfExpence!!.toString()
            )
        )
    }

    private fun submitData() {
        val amountResult = addExpenceIncomeVerifyingRepository.checkAmount(state.amount)
        val hasError = listOf(
            amountResult
        ).any { !it.successful }
        if (hasError) {
            state = state.copy(
                amountError = amountResult.errorMessage,
            )
            return
        }


        viewModelScope.launch {
            validationEventChannel.send(ValidationEvent.Success)
        }
    }

    private fun finalOfSaving() {
        viewModelScope.launch {
            if (isItIncome) {
                addIncome()
            } else {
                addExpence()
            }
            userDatabaseUseCases.updateUser(user)
            firebaseUsersRegistrationRepository.updateUser(user)
        }
    }

    private fun setIncomeCaterogy(category: String): CategoryOfIncome {
        when (category) {
            "P2P"
            -> return CategoryOfIncome.P2P

            "Replenishment"
            -> return CategoryOfIncome.Replenishment

            "Other"
            -> return CategoryOfIncome.Other
        }
        return CategoryOfIncome.Other
    }


    private fun setExpenceCaterogy(category: String): CategoryOfExpence {
        when (category) {
            "Grocery"
            -> return CategoryOfExpence.Grocery

            "Entertainment"
            -> return CategoryOfExpence.Entertainment

            "Restaurants"
            -> return CategoryOfExpence.Restaurants

            "Electronic"
            -> return CategoryOfExpence.Electronic

            "Transport"
            -> return CategoryOfExpence.Transport

            "Healthcare"
            -> return CategoryOfExpence.Healthcare

            "Other"
            -> return CategoryOfExpence.Other
        }
        return CategoryOfExpence.Other
    }

    sealed class ValidationEvent {
        data object Success : ValidationEvent()

    }
}